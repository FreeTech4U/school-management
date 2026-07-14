-- =============================================================================
-- V1__init_public_schema.sql
-- Schema PUBLIC — Gestion de la plateforme SaaS
--
-- Ce schema est UNIQUE et PARTAGÉ par toutes les écoles. Il contient la
-- « méta-donnée » de la plateforme : quelles écoles existent, quel abonnement
-- elles ont souscrit, et ce qu'elles ont payé à SchoolSaaS.
--
-- Les données métier de chaque école (élèves, notes, frais de scolarité) vivent
-- dans leur propre schema tenant — voir V1__init_tenant_schema.sql.
--
-- -----------------------------------------------------------------------------
-- NE PAS CONFONDRE LES DEUX FLUX D'ARGENT
-- -----------------------------------------------------------------------------
--   • Schema PUBLIC  : l'ÉCOLE paie SCHOOLSAAS (abonnement)
--                      → subscription_payments
--   • Schema TENANT  : le PARENT paie l'ÉCOLE  (frais de scolarité)
--                      → payments / payment_allocations
--
-- Ces deux flux ont des enums distincts, volontairement :
--   SubscriptionPaymentMethod / SubscriptionPaymentStatus  (public)
--   PaymentMethod             / PaymentStatus              (tenant)
-- Ils peuvent diverger sans se contraindre l'un l'autre.
--
-- -----------------------------------------------------------------------------
-- CONVENTIONS (identiques au schema tenant)
-- -----------------------------------------------------------------------------
--   • Énumérations en VARCHAR + CHECK, valeurs en MAJUSCULES.
--     CORRECTION MAJEURE : le script précédent utilisait des minuscules
--     ('trial', 'active', 'cash'...). Un enum Java annoté
--     @Enumerated(EnumType.STRING) écrit le NOM de la constante, donc TRIAL.
--     La contrainte CHECK aurait rejeté le premier INSERT dès que School.status
--     serait passé de String à enum — ce qui est le cas partout ailleurs.
--
--   • TIMESTAMPTZ ↔ Instant · DATE ↔ LocalDate
--
--   • gen_random_uuid() (natif PostgreSQL 13+), et non uuid_generate_v4() qui
--     dépend de l'extension uuid-ossp.
-- =============================================================================


-- =============================================================================
-- 0. FONCTION UTILITAIRE
-- =============================================================================

-- Met à jour automatiquement updated_at à chaque UPDATE.
--
-- Cette fonction est également redéfinie dans chaque schema tenant : Flyway
-- n'inclut pas public dans le search_path des migrations tenant, celle-ci y
-- serait donc introuvable.
CREATE OR REPLACE FUNCTION fn_update_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =============================================================================
-- 1. RÔLES — Catalogue partagé par toutes les écoles
--
-- AJOUT — remplace l'ancien enum Java fixe com.schoolsaas.common.enums.Role.
--
--   Les rôles ne sont plus un vocabulaire figé au moment de la compilation :
--   ils deviennent une DONNÉE, modifiable via une future API d'administration
--   (ajout d'un rôle "INFIRMIER", par exemple, sans nouvelle migration ni
--   redéploiement).
--
--   Cette table vit UNE SEULE FOIS ici, dans le schema public — jamais
--   dupliquée dans chaque schema tenant. Le vocabulaire des rôles est celui
--   du PRODUIT SchoolSaaS, pas une personnalisation propre à chaque école.
--   <schema_tenant>.user_roles.role_id la référence par une clé étrangère
--   INTER-SCHEMA (PostgreSQL l'autorise pleinement au sein d'une même base) :
--   un seul catalogue, modifiable une fois, utilisable par toutes les écoles
--   immédiatement, sans qu'aucun schema tenant n'ait à être touché.
--
--   La suppression d'un rôle est modélisée comme une DÉSACTIVATION
--   (is_active = false), jamais une suppression réelle : un DELETE sur une
--   ligne déjà référencée par des user_roles dans 50 schemas différents
--   casserait ces 50 écoles d'un coup. Cohérent avec is_active déjà utilisé
--   sur subjects et sms_templates.
-- =============================================================================

CREATE TABLE roles (
                       id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Code stable, utilisé par le code applicatif et par le JWT (claim "roles").
    -- Convention : MAJUSCULES, ex : DIRECTOR, TEACHER, INFIRMIER.
                       code       VARCHAR(30)  UNIQUE NOT NULL,

                       label      VARCHAR(100) NOT NULL,

    -- Désactivation plutôt que suppression — voir note ci-dessus.
                       is_active  BOOLEAN      NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT chk_role_code CHECK (code ~ '^[A-Z_]+$')
    );


-- =============================================================================
-- 2. PLANS D'ABONNEMENT
--
-- Catalogue tarifaire de SchoolSaaS. Trois plans, tarifés en GNF.
--
-- CORRECTION — ambiguïté price_monthly / billing_cycle :
--   Le script précédent portait une seule colonne price_monthly, plus un
--   billing_cycle sur le PLAN. Pour un plan YEARLY, que valait price_monthly ?
--   Le prix mensuel à multiplier par 12, ou le prix annuel mal nommé ?
--   Personne ne pouvait le deviner.
--
--   Modèle retenu : le plan porte DEUX prix (mensuel et annuel), et c'est
--   l'ABONNEMENT qui porte le cycle de facturation choisi par l'école. Deux
--   écoles peuvent ainsi souscrire au même plan, l'une au mois, l'autre à
--   l'année — ce que le modèle précédent interdisait.
--
-- Enum Java associé : (aucun — le plan est une donnée, pas une énumération)
-- =============================================================================

CREATE TABLE subscription_plans (
                                    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    code          VARCHAR(50)    UNIQUE NOT NULL,   -- starter | standard | premium
                                    name          VARCHAR(100)   NOT NULL,
                                    description   TEXT,

    -- Deux prix distincts, sans ambiguïté possible.
    -- Le prix annuel est volontairement inférieur à 12 × le prix mensuel :
    -- la remise est l'incitation commerciale à l'engagement annuel.
                                    price_monthly DECIMAL(19, 2) NOT NULL,
                                    price_yearly  DECIMAL(19, 2) NOT NULL,

                                    currency      VARCHAR(3)     NOT NULL DEFAULT 'GNF',

    -- NULL = illimité (plan Premium).
                                    max_students  INTEGER,

    -- Quota de SMS inclus par mois.
    -- Le compteur de consommation vit sur school_subscriptions.sms_used_this_month.
                                    sms_included  INTEGER        NOT NULL DEFAULT 100,

                                    features      JSONB          NOT NULL DEFAULT '{}'::jsonb,
                                    is_active     BOOLEAN        NOT NULL DEFAULT TRUE,
                                    created_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT chk_plan_prices       CHECK (price_monthly >= 0 AND price_yearly >= 0),
                                    CONSTRAINT chk_plan_max_students CHECK (max_students IS NULL OR max_students > 0),
                                    CONSTRAINT chk_plan_sms_included CHECK (sms_included >= 0),
    -- Le tarif annuel doit rester avantageux, sinon l'engagement n'a pas de sens.
                                    CONSTRAINT chk_plan_yearly_discount CHECK (price_yearly <= price_monthly * 12)
);


-- =============================================================================
-- 3. ÉCOLES (= TENANTS)
--
-- Chaque ligne correspond à un schema PostgreSQL isolé (schema_name).
--
-- Enum Java associé : SchoolStatus
-- =============================================================================

CREATE TABLE schools (
                         id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         name         VARCHAR(255) NOT NULL,
                         slug         VARCHAR(255) UNIQUE NOT NULL,      -- ex : ste-marie-dixinn
    -- sert à identifier le tenant au login

    -- VARCHAR(63) : limite stricte de PostgreSQL pour un identifiant.
    -- Au-delà, le nom serait tronqué silencieusement.
                         schema_name  VARCHAR(63)  UNIQUE NOT NULL,      -- ex : tenant_ste_marie

    -- CORRECTION — UNIQUE retiré.
    -- Un propriétaire de plusieurs écoles utilise couramment le MÊME email de
    -- contact pour chacune (celui du directeur). L'unicité réelle de
    -- l'identité d'une personne est portée par persons.email, pas par ce
    -- champ, qui n'est qu'un contact administratif propre à CETTE école.
    -- Garder UNIQUE ici aurait bloqué la création de la deuxième école d'un
    -- même propriétaire — exactement le scénario que ce modèle doit permettre.
                         email        VARCHAR(255) NOT NULL,
                         phone        VARCHAR(20),
                         address      TEXT,
                         city         VARCHAR(100),
                         country_code VARCHAR(3)   NOT NULL DEFAULT 'GN',
                         logo_url     VARCHAR(255),
                         status       VARCHAR(20)  NOT NULL DEFAULT 'TRIAL',

    -- Fuseau horaire de l'école. C'est une DONNÉE MÉTIER, pas un réglage
    -- technique : les schedulers (rappels SMS à 8h, bascule OVERDUE à minuit)
    -- se déclenchent toutes les heures en UTC et filtrent les écoles pour
    -- lesquelles il est actuellement l'heure locale voulue.
    -- Une école à Conakry et une à Abidjan reçoivent ainsi leurs SMS à 8h
    -- CHEZ ELLES, avec un seul scheduler.
                         timezone     VARCHAR(50)  NOT NULL DEFAULT 'Africa/Conakry',

                         currency     VARCHAR(3)   NOT NULL DEFAULT 'GNF',
                         created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Enum Java : SchoolStatus
    --   TRIAL     : période d'essai (30 jours), accès complet
    --   ACTIVE    : abonnement payé et en cours
    --   SUSPENDED : suspendu pour impayé — données conservées, accès bloqué
    --   DELETED   : résilié — le schema tenant est conservé pour la rétention légale
    --
    -- TRIAL et ACTIVE sont les états « opérationnels » : ce sont les seules
    -- écoles dont TenantInitializer applique les migrations et dont les
    -- schedulers traitent les données.
                         CONSTRAINT chk_school_status CHECK (
                             status IN ('TRIAL', 'ACTIVE', 'SUSPENDED', 'DELETED')
                             ),

    -- Garde-fou anti-injection SQL. Le nom du schema est concaténé dans des
    -- requêtes (SET search_path, CREATE SCHEMA, REFRESH MATERIALIZED VIEW) car
    -- PostgreSQL n'autorise PAS les paramètres préparés (?) pour les noms
    -- d'objets. Cette contrainte garantit qu'aucun caractère dangereux ne peut
    -- entrer en base. Elle double la validation regex faite côté Java.
                         CONSTRAINT chk_school_schema_name CHECK (schema_name ~ '^[a-z0-9_]+$'),

    CONSTRAINT chk_school_slug CHECK (slug ~ '^[a-z0-9-]+$')
);


-- =============================================================================
-- 4. ABONNEMENTS DES ÉCOLES
--
-- Enums Java associés : SubscriptionStatus, BillingCycle
-- =============================================================================

CREATE TABLE school_subscriptions (
                                      id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      school_id            UUID        NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
                                      plan_id              UUID        NOT NULL REFERENCES subscription_plans (id),

    -- Le cycle de facturation est porté par l'ABONNEMENT, pas par le plan :
    -- deux écoles peuvent souscrire au même plan, l'une au mois, l'autre à
    -- l'année. Il détermine lequel des deux prix du plan s'applique.
                                      billing_cycle        VARCHAR(20) NOT NULL DEFAULT 'YEARLY',

                                      start_date           DATE        NOT NULL,
                                      end_date             DATE        NOT NULL,
                                      status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                      auto_renew           BOOLEAN     NOT NULL DEFAULT TRUE,

    -- Compteur de SMS consommés sur le mois en cours.
    --
    -- CORRECTION : le champ sms_included existait sur le plan, mais AUCUN
    -- compteur ne mesurait la consommation réelle. Le quota était donc
    -- inapplicable — impossible de bloquer un dépassement ou de facturer un
    -- surplus. Ce compteur, remis à zéro chaque mois par un scheduler,
    -- rend le quota exploitable.
                                      sms_used_this_month  INTEGER     NOT NULL DEFAULT 0,
                                      sms_counter_reset_at DATE        NOT NULL DEFAULT CURRENT_DATE,

                                      created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Enum Java : SubscriptionStatus
    --   ACTIVE    : en cours de validité
    --   EXPIRED   : date de fin dépassée sans renouvellement
    --   CANCELLED : résilié à la demande de l'école, avant sa date de fin
                                      CONSTRAINT chk_subscription_status CHECK (
                                          status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')
                                          ),

    -- Enum Java : BillingCycle
    --   MONTHLY : facturation mensuelle (1 mois)
    --   YEARLY  : facturation annuelle (12 mois) — mode privilégié
                                      CONSTRAINT chk_subscription_billing_cycle CHECK (
                                          billing_cycle IN ('MONTHLY', 'YEARLY')
                                          ),

                                      CONSTRAINT chk_subscription_dates CHECK (end_date > start_date),
                                      CONSTRAINT chk_subscription_sms   CHECK (sms_used_this_month >= 0)
);

-- CORRECTION CRITIQUE — une école ne peut avoir qu'UN SEUL abonnement actif.
--
-- Sans cette contrainte, un renouvellement interrompu à mi-parcours ou un
-- double-clic sur « Renouveler » créait deux abonnements ACTIVE pour la même
-- école. Quel plan s'appliquait alors ? Le premier retourné par la base —
-- résultat non déterministe, et facturation potentiellement fausse.
--
-- Index UNIQUE PARTIEL : la contrainte ne porte que sur les lignes ACTIVE.
-- L'historique des abonnements EXPIRED et CANCELLED reste illimité.
CREATE UNIQUE INDEX uq_school_active_subscription
    ON school_subscriptions (school_id)
    WHERE status = 'ACTIVE';


-- =============================================================================
-- 5. PAIEMENTS D'ABONNEMENT
--
-- L'ÉCOLE paie SCHOOLSAAS. À ne pas confondre avec le schema tenant, où le
-- PARENT paie l'ÉCOLE.
--
-- Enums Java associés : SubscriptionPaymentStatus, SubscriptionPaymentMethod
-- =============================================================================

CREATE TABLE subscription_payments (
                                       id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       subscription_id       UUID           NOT NULL REFERENCES school_subscriptions (id)
                                           ON DELETE CASCADE,
                                       amount                DECIMAL(19, 2) NOT NULL,
                                       currency              VARCHAR(3)     NOT NULL DEFAULT 'GNF',

    -- DATE et non TIMESTAMPTZ : un paiement a une date, pas une heure précise.
                                       payment_date          DATE           NOT NULL DEFAULT CURRENT_DATE,

                                       payment_method        VARCHAR(20)    NOT NULL,
                                       transaction_reference VARCHAR(100)   UNIQUE,
                                       status                VARCHAR(20)    NOT NULL DEFAULT 'COMPLETED',

    -- Période couverte par ce paiement. Sans ces colonnes, impossible de savoir
    -- si un versement couvre un mois ou une année, ni de produire un échéancier.
                                       period_start          DATE,
                                       period_end            DATE,

                                       notes                 TEXT,
                                       created_at            TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at            TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Enum Java : SubscriptionPaymentStatus
    --   PENDING   : annoncé, en attente de confirmation bancaire
    --               (un virement peut être annoncé avant d'être crédité)
    --   COMPLETED : encaissé
    --   FAILED    : échoué (virement rejeté, transaction mobile money annulée)
    --   REFUNDED  : remboursé à l'école
                                       CONSTRAINT chk_sub_payment_status CHECK (
                                           status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')
                                           ),

    -- Enum Java : SubscriptionPaymentMethod
    --   BANK_TRANSFER : virement — mode dominant pour la facturation annuelle
    --   ORANGE_MONEY / MTN_MONEY / WAVE : mobile money
    --   CARD          : carte bancaire (passerelle en ligne)
    --   CHECK         : chèque
    --
    -- CASH est volontairement ABSENT : SchoolSaaS est opéré à distance, il n'y
    -- a pas de guichet. C'est la différence structurelle avec PaymentMethod
    -- (schema tenant), où CASH est au contraire le mode dominant.
                                       CONSTRAINT chk_sub_payment_method CHECK (
                                           payment_method IN ('BANK_TRANSFER', 'ORANGE_MONEY', 'MTN_MONEY',
                                                              'WAVE', 'CARD', 'CHECK')
                                           ),

                                       CONSTRAINT chk_sub_payment_amount CHECK (amount > 0),
                                       CONSTRAINT chk_sub_payment_period CHECK (
                                           period_start IS NULL OR period_end IS NULL OR period_end > period_start
                                           )
);


-- =============================================================================
-- 6. IDENTITÉ TRANSVERSE — Personnes et appartenances aux écoles
--
-- AJOUT — répond à un besoin produit précis : une même personne (propriétaire
-- de plusieurs écoles, enseignant intervenant dans plusieurs établissements)
-- doit pouvoir se connecter UNE FOIS et accéder à toutes ses écoles, avec un
-- changement d'école SANS reconnexion ni nouveau mot de passe.
--
-- ⚠ ATTENTION — persons ne contient AUCUN mot de passe. L'authentification
--   reste entièrement portée par <schema_tenant>.users (password_hash), une
--   fois pour chaque école. persons ne sert qu'à répondre à une question :
--   « à quelles écoles cet email a-t-il accès, et laquelle a-t-il visitée en
--   dernier ? » — jamais à vérifier une identité par mot de passe.
--
--   Centraliser les mots de passe ici aurait fait de cette table une cible
--   unique dont la compromission exposerait TOUTES les écoles de la
--   plateforme d'un coup — l'isolation par schema, qui est la principale
--   garantie de sécurité du projet, aurait perdu tout son sens.
--
-- Le rôle (DIRECTOR, TEACHER...) n'apparaît PAS ici : il reste porté par
-- <schema_tenant>.user_roles, propre à chaque école. roles_snapshot ci-dessous
-- n'est qu'un résumé d'affichage dénormalisé (ex: "DIRECTOR, TEACHER"), utile
-- pour peupler un sélecteur d'écoles sans devoir interroger chaque tenant.
-- =============================================================================

CREATE TABLE persons (
                         id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         email                     VARCHAR(255) UNIQUE NOT NULL,

    -- Pilote la redirection automatique après connexion (F-02).
    -- Nullable seulement en théorie : en pratique, l'onboarding (F-01)
    -- positionne toujours cette valeur dès la création de la première école,
    -- donc une personne existante n'a jamais ce champ à NULL.
    -- ON DELETE SET NULL : si cette école précise est supprimée, on retombe
    -- proprement sur le mécanisme de repli plutôt que sur une FK cassée.
                         last_connected_school_id UUID REFERENCES schools (id) ON DELETE SET NULL,

                         created_at                TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at                TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Appartenance d'une personne à une école, avec un résumé de ses rôles.
CREATE TABLE school_memberships (
                                    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    person_id       UUID NOT NULL REFERENCES persons (id) ON DELETE CASCADE,
                                    school_id       UUID NOT NULL REFERENCES schools (id)  ON DELETE CASCADE,

    -- Pointe vers la ligne réelle dans <schema_tenant>.users. Indispensable
    -- pour le changement d'école sans reconnexion (switch-school) : on sait
    -- directement QUEL utilisateur du tenant charger, sans avoir à
    -- rechercher par email dans le schema cible.
                                    tenant_user_id  UUID NOT NULL,

    -- Résumé d'affichage uniquement (ex: "DIRECTOR, TEACHER"), synchronisé à
    -- chaque changement de rôle côté tenant. Ne fait PAS foi pour les
    -- autorisations — celles-ci sont vérifiées via le JWT émis après
    -- résolution du tenant, à partir de user_roles.
                                    roles_snapshot  VARCHAR(200),

    -- Coupe-circuit propre à CETTE école : un directeur peut désactiver
    -- l'accès d'une personne à SON école sans toucher à ses autres
    -- appartenances. Distinct de schools.status (qui coupe TOUTE l'école).
                                    is_active       BOOLEAN NOT NULL DEFAULT TRUE,

                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Une personne n'a qu'une seule appartenance par école (pas de doublons).
                                    CONSTRAINT uq_person_school UNIQUE (person_id, school_id)
);


-- =============================================================================
-- 7. INDEX DE PERFORMANCE
--
-- PostgreSQL indexe automatiquement les clés primaires et les contraintes
-- UNIQUE, mais PAS les clés étrangères.
-- =============================================================================

-- Sert la future API d'administration des rôles ("lister les rôles actifs")
-- et le formulaire d'affectation de rôle à un utilisateur.
CREATE INDEX idx_roles_active ON roles (is_active) WHERE is_active = TRUE;

-- Sert TenantInitializer et les schedulers, qui parcourent les écoles
-- opérationnelles (TRIAL + ACTIVE) à chaque exécution.
CREATE INDEX idx_schools_status ON schools (status);

-- Sert AuthService : le login résout l'école par son slug avant tout accès.
-- (slug est déjà UNIQUE donc indexé — cet index serait redondant, il est omis.)

CREATE INDEX idx_school_subscriptions_school   ON school_subscriptions (school_id);
CREATE INDEX idx_school_subscriptions_status   ON school_subscriptions (status);

-- Sert le scheduler qui détecte les abonnements arrivant à échéance
-- (relance commerciale, suspension automatique).
CREATE INDEX idx_school_subscriptions_end_date ON school_subscriptions (end_date)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_subscription_payments_sub  ON subscription_payments (subscription_id);
CREATE INDEX idx_subscription_payments_date ON subscription_payments (payment_date);

-- Sert AuthService.login() : résout, pour un email donné, ses écoles actives.
-- Index PARTIEL : seules les appartenances actives sont consultées à la
-- connexion — les désactivées ne servent qu'à l'historique.
CREATE INDEX idx_school_memberships_person
    ON school_memberships (person_id)
    WHERE is_active = TRUE;

CREATE INDEX idx_school_memberships_school ON school_memberships (school_id);

-- Sert le switch-school : retrouver directement l'utilisateur tenant visé
-- sans recherche par email dans le schema cible.
CREATE INDEX idx_school_memberships_tenant_user ON school_memberships (tenant_user_id);


-- =============================================================================
-- 8. TRIGGERS updated_at
--
-- CORRECTION : ces triggers étaient absents du script d'origine. La colonne
-- updated_at existait mais n'était jamais mise à jour — elle conservait
-- indéfiniment la valeur posée à la création.
-- =============================================================================

CREATE TRIGGER trg_updated_at_roles
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_subscription_plans
    BEFORE UPDATE ON subscription_plans
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_schools
    BEFORE UPDATE ON schools
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_school_subscriptions
    BEFORE UPDATE ON school_subscriptions
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_subscription_payments
    BEFORE UPDATE ON subscription_payments
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_persons
    BEFORE UPDATE ON persons
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_school_memberships
    BEFORE UPDATE ON school_memberships
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();
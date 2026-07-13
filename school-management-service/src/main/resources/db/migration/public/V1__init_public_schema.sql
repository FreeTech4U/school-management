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
-- 1. PLANS D'ABONNEMENT
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
-- 2. ÉCOLES (= TENANTS)
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

    email        VARCHAR(255) UNIQUE NOT NULL,
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
-- 3. ABONNEMENTS DES ÉCOLES
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
-- 4. PAIEMENTS D'ABONNEMENT
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
-- 5. INDEX DE PERFORMANCE
--
-- PostgreSQL indexe automatiquement les clés primaires et les contraintes
-- UNIQUE, mais PAS les clés étrangères.
-- =============================================================================

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


-- =============================================================================
-- 6. TRIGGERS updated_at
--
-- CORRECTION : ces triggers étaient absents du script d'origine. La colonne
-- updated_at existait mais n'était jamais mise à jour — elle conservait
-- indéfiniment la valeur posée à la création.
-- =============================================================================

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


-- =============================================================================
-- 7. DONNÉES INITIALES — Catalogue tarifaire (GNF)
--
-- Référence de change : 1 EUR ≈ 9 500 GNF
--
--   Starter  :  45 000 GNF/mois  (≈  4,7 €)  →   450 000 GNF/an  (2 mois offerts)
--   Standard : 120 000 GNF/mois  (≈ 12,6 €)  → 1 200 000 GNF/an  (2 mois offerts)
--   Premium  : 280 000 GNF/mois  (≈ 29,5 €)  → 2 800 000 GNF/an  (2 mois offerts)
--
-- La remise annuelle (2 mois offerts) est l'incitation à l'engagement : elle
-- sécurise la trésorerie et réduit mécaniquement le taux de résiliation.
-- =============================================================================

INSERT INTO subscription_plans
    (code, name, description, price_monthly, price_yearly,
     max_students, sms_included, features)
VALUES
(
    'starter',
    'Starter',
    'Pour les petites écoles jusqu''à 200 élèves',
    45000.00,
    450000.00,          -- 10 mois payés au lieu de 12
    200,
    200,
    '{"timetable": false, "advanced_reports": false, "parent_app": false}'::jsonb
),
(
    'standard',
    'Standard',
    'Pour les écoles de 200 à 600 élèves',
    120000.00,
    1200000.00,
    600,
    600,
    '{"timetable": true, "advanced_reports": true, "parent_app": false}'::jsonb
),
(
    'premium',
    'Premium',
    'Pour les grandes écoles et groupes scolaires — élèves illimités',
    280000.00,
    2800000.00,
    NULL,               -- illimité
    2000,
    '{"timetable": true, "advanced_reports": true, "parent_app": true}'::jsonb
);

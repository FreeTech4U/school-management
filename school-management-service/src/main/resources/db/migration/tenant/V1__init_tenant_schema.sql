-- =============================================================================
-- V1__init_tenant_schema.sql
-- Schema TENANT — Données métier d'UNE école
--
-- Ce script est rejoué pour CHAQUE nouvelle école lors de l'onboarding (F-01).
-- Il crée un schema PostgreSQL isolé contenant l'intégralité des données de
-- l'établissement : élèves, notes, paiements, emploi du temps.
--
-- -----------------------------------------------------------------------------
-- CONVENTION DE MODÉLISATION DES ÉNUMÉRATIONS  ← LIRE AVANT TOUTE MODIFICATION
-- -----------------------------------------------------------------------------
-- Toutes les énumérations sont modélisées en VARCHAR + contrainte CHECK,
-- et NON en types ENUM natifs PostgreSQL (CREATE TYPE ... AS ENUM).
--
-- Pourquoi ce choix :
--
--   1. MAPPING JAVA SANS ANNOTATION EXOTIQUE
--      Les entités utilisent des enums Java annotés simplement :
--          @Enumerated(EnumType.STRING)
--          @Column(nullable = false)
--          private FeeStatus status;
--      Avec un type ENUM natif PostgreSQL, il faudrait ajouter
--      @JdbcTypeCode(SqlTypes.NAMED_ENUM) sur CHAQUE champ de CHAQUE entité.
--      L'oublier une seule fois provoque, au premier INSERT :
--          ERROR: column "status" is of type fee_status
--                 but expression is of type character varying
--
--   2. ÉVOLUTIVITÉ EN CONTEXTE MULTITENANT
--      Un type ENUM PostgreSQL est défini PAR SCHEMA. Avec 14 énumérations et
--      50 écoles, cela ferait 700 types à maintenir. Ajouter une valeur
--      (ex : un nouvel opérateur mobile money) imposerait un ALTER TYPE dans
--      chacun des 50 schemas — commande qui ne peut pas s'exécuter dans une
--      transaction sur les anciennes versions de PostgreSQL, ce qui complique
--      la migration Flyway.
--      Avec une contrainte CHECK : un simple DROP CONSTRAINT / ADD CONSTRAINT,
--      transactionnel et trivial.
--
--   3. INTÉGRITÉ ÉQUIVALENTE
--      La contrainte CHECK garantit exactement la même chose qu'un type ENUM :
--      aucune valeur hors liste ne peut être insérée.
--
-- Chaque contrainte CHECK est alignée 1:1 sur l'enum Java correspondant dans
-- com.schoolsaas.common.enums. Toute valeur ajoutée d'un côté DOIT l'être de
-- l'autre.
--
-- EXCEPTION — user_roles.role_id.
--   Contrairement aux autres énumérations, le vocabulaire des rôles n'est
--   PAS fixe : il doit pouvoir être étendu à l'exécution via une future API
--   d'administration (ajouter un rôle "INFIRMIER", par exemple, sans
--   migration). role_id est donc une clé étrangère INTER-SCHEMA vers une
--   vraie table (public.roles), et non un VARCHAR + CHECK. Voir le
--   commentaire sur la table user_roles, section 1, et
--   V1__init_public_schema.sql section 1 (RÔLES).
--
-- -----------------------------------------------------------------------------
-- CONVENTION DE TYPAGE TEMPOREL
-- -----------------------------------------------------------------------------
--   TIMESTAMPTZ  ↔  java.time.Instant     (horodatage absolu, sans ambiguïté)
--   DATE         ↔  java.time.LocalDate   (date pure : naissance, échéance)
--   TIME         ↔  java.time.LocalTime   (heure pure : créneaux horaires)
--
-- Ne JAMAIS utiliser LocalDateTime pour une colonne TIMESTAMPTZ : ce type n'a
-- pas de fuseau, Hibernate lui applique implicitement celui de la JVM, ce qui
-- provoque des décalages silencieux entre le poste de dev et le serveur.
--
-- -----------------------------------------------------------------------------
-- POINT D'ATTENTION FLYWAY
-- -----------------------------------------------------------------------------
-- Ce script s'exécute avec search_path positionné sur le schema tenant.
-- La fonction fn_update_updated_at() y est donc redéfinie, afin que le script
-- soit autonome et ne dépende pas de la présence du schema public.
--
-- Les identifiants sont générés avec gen_random_uuid(), natif depuis
-- PostgreSQL 13 — aucune extension requise (contrairement à uuid_generate_v4()
-- qui dépend de l'extension uuid-ossp installée dans le schema public).
-- =============================================================================
-- =============================================================================
-- 0. FONCTION UTILITAIRE
-- =============================================================================
-- Met à jour automatiquement la colonne updated_at à chaque UPDATE.
--
-- Cette fonction est redéfinie dans le schema tenant (et non appelée depuis le
-- schema public) car Flyway n'inclut pas public dans le search_path des
-- migrations tenant : la fonction du schema public serait introuvable au moment
-- de créer les triggers, l'erreur provoquerait un rollback de TOUTE la migration
-- (le DDL PostgreSQL est transactionnel), et le schema resterait vide.
CREATE OR REPLACE FUNCTION fn_update_updated_at()
    RETURNS TRIGGER
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- =============================================================================
-- 1. IDENTITY — Utilisateurs et enseignants (F-02, F-03)
-- =============================================================================
-- Utilisateurs de l'application (personnel de l'école).
--
-- CORRECTION — le rôle n'est plus une colonne unique sur users.
--   Une même personne peut désormais cumuler plusieurs rôles dans la MÊME
--   école (ex : comptable ET enseignant dans une petite structure), ou être
--   DIRECTOR d'une école et TEACHER d'une autre. Le rôle devient donc une
--   relation PLUSIEURS-À-PLUSIEURS avec users, portée par la table user_roles
--   ci-dessous — et non par une table de référence "roles" séparée : le
--   projet évite délibérément les tables de référence pour les énumérations
--   fixes (même logique que fee_type ou payment_method : VARCHAR + CHECK,
--   jamais de table de correspondance avec clé de substitution).
CREATE TABLE users(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL, -- BCrypt, force 12
    avatar_url VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- désactivation = soft delete
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rôles attribués à un utilisateur, DANS ce tenant (F-02, F-03).
--
-- CORRECTION — clé primaire composite (user_id, role_id) remplacée par un id
-- surrogate + contrainte UNIQUE, pour rester cohérent avec TOUTES les autres
-- tables de jointure du schéma (class_subjects, payment_allocations...), qui
-- suivent systématiquement cette convention plutôt qu'une PK composite.
--
-- role_id (FK inter-schema vers public.roles) : le vocabulaire des rôles
-- n'est plus figé au moment de la migration, il vit dans une table unique
-- partagée par toutes les écoles, modifiable via une future API
-- d'administration sans toucher à aucune migration tenant.
CREATE TABLE user_roles(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- Référence INTER-SCHEMA : la table roles vit une seule fois dans public,
    -- jamais dupliquée dans chaque tenant (voir V1__init_public_schema.sql).
    role_id UUID NOT NULL REFERENCES public.roles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

-- Informations complémentaires propres aux enseignants.
-- Une ligne est créée automatiquement par UserService lorsqu'un utilisateur
-- est créé avec le rôle TEACHER (F-03).
-- La PK est aussi la FK : relation 1-1 stricte avec users.
CREATE TABLE teachers(
    id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    employee_number VARCHAR(50) UNIQUE,
    hire_date DATE,
    specialty VARCHAR(100),
    qualification VARCHAR(255),
    bio TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 2. ACADEMIC — Structure pédagogique (F-04 à F-07)
-- =============================================================================
-- Année scolaire. Une seule peut être courante à la fois.
-- Enum Java associé : YearStatus
CREATE TABLE academic_years(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label VARCHAR(50) UNIQUE NOT NULL, -- ex : "2024-2025"
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : YearStatus
    --   ACTIVE : en cours
    --   CLOSED : clôturée — l'année n'est plus modifiable, sa clôture déclenche
    --            la validation des promotions en attente (F-19)
    CONSTRAINT chk_year_status CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT chk_year_dates CHECK (end_date > start_date)
);

-- Une seule année scolaire courante (F-04).
-- Index UNIQUE partiel : la contrainte ne porte que sur les lignes à TRUE,
-- toutes les autres années peuvent être à FALSE sans conflit.
CREATE UNIQUE INDEX uq_academic_year_current ON academic_years(is_current)
WHERE
    is_current = TRUE;

-- Trimestre. Le drapeau grades_entry_open contrôle la saisie des notes.
CREATE TABLE terms(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL, -- ex : "1er Trimestre"
    term_number SMALLINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    -- Seul le directeur ouvre et ferme la saisie. Un enseignant ne peut saisir
    -- une note que si ce drapeau vaut TRUE (F-05, code erreur GRADES_ENTRY_CLOSED).
    grades_entry_open BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_term_number_per_year UNIQUE (academic_year_id, term_number),
    CONSTRAINT chk_term_number CHECK (term_number BETWEEN 1 AND 4),
    CONSTRAINT chk_term_dates CHECK (end_date > start_date)
);

-- Un seul trimestre courant PAR ANNÉE.
CREATE UNIQUE INDEX uq_term_current_per_year ON terms(academic_year_id)
WHERE
    is_current = TRUE;

-- Niveaux du cursus (Primaire, Collège, Lycée).
-- order_index définit l'ordre de progression, utilisé par la promotion (F-19).
CREATE TABLE levels(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    order_index SMALLINT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Classe. Entité Java : SchoolClass — « Class » est un mot réservé Java,
-- d'où l'annotation @Table(name = "classes") côté entité.
CREATE TABLE classes(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    level_id UUID NOT NULL REFERENCES levels(id),
    name VARCHAR(100) NOT NULL, -- ex : "6ème A"
    option VARCHAR(100), -- Scientifique, Littéraire...
    capacity SMALLINT,
    room_number VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_class_name_per_year UNIQUE (academic_year_id, NAME),
    CONSTRAINT chk_class_capacity CHECK (capacity IS NULL OR capacity > 0)
);

-- Matières enseignées dans l'établissement.
CREATE TABLE subjects(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    code VARCHAR(20) UNIQUE, -- ex : "MATH"
    color VARCHAR(7), -- #RRGGBB, pour l'emploi du temps
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subject_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

-- Affectation matière ↔ classe ↔ enseignant.
-- Le coefficient est utilisé dans le calcul de la moyenne générale (F-16) :
--   moyenne = Σ(moyenne_matière × coefficient) / Σ(coefficients)
CREATE TABLE class_subjects(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id),
    -- Nullable : une matière peut être créée avant qu'un enseignant y soit affecté.
    -- ON DELETE SET NULL : désactiver un enseignant ne supprime pas la matière.
    teacher_id UUID REFERENCES users(id) ON DELETE SET NULL,
    coefficient SMALLINT NOT NULL DEFAULT 1,
    weekly_hours SMALLINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_class_subject UNIQUE (class_id, subject_id),
    CONSTRAINT chk_coefficient CHECK (coefficient BETWEEN 1 AND 10),
    CONSTRAINT chk_weekly_hours CHECK (weekly_hours IS NULL OR weekly_hours > 0)
);

-- =============================================================================
-- 3. ENROLLMENT — Élèves et inscriptions (F-08, F-09, F-19)
-- =============================================================================
-- Fiche élève.
--
-- DIVERGENCE ASSUMÉE vs diagramme de classes : le diagramme prévoyait un champ
-- status (StudentStatus : ACTIVE / LEFT / GRADUATED). Il n'est PAS implémenté,
-- volontairement.
--
-- Raison : ce statut est une donnée DÉRIVÉE de la dernière inscription de
-- l'élève (enrollments.status porte déjà WITHDRAWN et GRADUATED). Le dupliquer
-- sur la fiche élève créerait deux sources de vérité potentiellement
-- contradictoires. Le cas qui casse le modèle : un élève parti en 2024 (LEFT)
-- qui se réinscrit en 2026 — que vaut alors students.status, sachant que son
-- inscription 2024 reste WITHDRAWN ?
--
-- La colonne is_active ci-dessous est un simple drapeau de soft delete
-- (« ne plus afficher cet élève dans les listes »), ce qui est un besoin
-- technique distinct du parcours scolaire.
--
-- Pour connaître le parcours : requêter enrollments, seule source de vérité.
CREATE TABLE students(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_number VARCHAR(20) UNIQUE, -- EL-YYYY-NNNN, généré par trigger
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    -- Nullable : en Guinée, une inscription se fait couramment sans que la
    -- famille dispose de tous les documents. Rendre ces champs obligatoires
    -- forcerait la saisie de données fausses par les comptables.
    date_of_birth DATE,
    gender CHAR(1),
    birth_city VARCHAR(100),
    birth_country VARCHAR(3) DEFAULT 'GN',
    photo_url VARCHAR(255),
    address TEXT,
    parent_name VARCHAR(255),
    parent_phone VARCHAR(20), -- format international +224XXXXXXXXX
    medical_notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_student_gender CHECK (gender IS NULL OR gender IN ('M', 'F'))
);

-- Inscription d'un élève dans une classe pour une année scolaire donnée.
-- C'est la table pivot du modèle : notes, présences et frais s'y rattachent.
--
-- Enums Java associés : EnrollmentStatus, PromotionStatus
CREATE TABLE enrollments(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    is_repeating BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    promotion_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Traçabilité de la décision de passage (F-19) : qui a validé, et quand.
    -- Ces deux colonnes rendent inutile un statut « OVERRIDDEN » : un override
    -- du directeur produit un statut FINAL (PROMOTED ou REPEATED) et laisse
    -- sa signature ici.
    promotion_validated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    promotion_validated_at TIMESTAMPTZ,
    transfer_notes VARCHAR(500), -- obligatoire si status = TRANSFERRED
    final_average DECIMAL(5, 2), -- moyenne annuelle, base du passage
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : EnrollmentStatus
    --   ENROLLED    : inscrit et actif — SEUL statut comptant dans les effectifs
    --                 et dans la génération des frais
    --   TRANSFERRED : parti vers un autre établissement
    --   WITHDRAWN   : a quitté l'école en cours d'année (abandon, départ famille)
    --   GRADUATED   : a terminé le cycle
    -- ATTENTION : la vue matérialisée mv_dashboard_stats filtre sur 'ENROLLED'.
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ENROLLED', 'TRANSFERRED', 'WITHDRAWN', 'GRADUATED')),
    -- Enum Java : PromotionStatus
    --   PENDING   : décision non encore prise (valeur par défaut)
    --   PROMOTED  : admis en classe supérieure
    --   REPEATED  : redouble (réinscription dans la même classe)
    --   GRADUATED : fin de cycle (dernière classe du dernier niveau)
    CONSTRAINT chk_promotion_status CHECK (promotion_status IN ('PENDING', 'PROMOTED', 'REPEATED', 'GRADUATED')),
    -- Une seule inscription par élève et par année scolaire (F-09).
    CONSTRAINT uq_enrollment_per_year UNIQUE (student_id, academic_year_id),
    CONSTRAINT chk_final_average CHECK (final_average IS NULL OR final_average BETWEEN 0 AND 20)
);

-- Traitement des promotions de fin d'année, classe par classe (F-19).
--
-- Cette table n'est PAS une donnée métier : c'est un journal d'orchestration.
-- Elle permet de dérouler une promotion en trois temps, au lieu d'un bouton
-- irréversible qui écrirait directement dans enrollments :
--
--   CREATED   → le système calcule les propositions (moyenne ≥ seuil → PROMOTED,
--               sinon REPEATED). Rien n'est encore écrit dans enrollments.
--   VALIDATED → le directeur a relu, ajusté les cas limites, et confirmé.
--   EXECUTED  → les inscriptions de l'année suivante sont réellement créées.
--   CANCELLED → le directeur a abandonné le traitement.
--
-- validation_errors porte les blocages empêchant l'exécution
-- (ex : « 3 élèves sans moyenne finale — bulletins du T3 non publiés »).
--
-- NOTE : prévu pour la phase 3. La table est créée dès maintenant, mais le code
-- Java correspondant n'est pas implémenté dans le MVP.
CREATE TABLE promotion_batches(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    next_academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    promoted_count INTEGER NOT NULL DEFAULT 0,
    repeated_count INTEGER NOT NULL DEFAULT 0,
    graduated_count INTEGER NOT NULL DEFAULT 0,
    total_processed INTEGER NOT NULL DEFAULT 0,
    validation_errors TEXT,
    executed_at TIMESTAMPTZ,
    notes TEXT,
    director_comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_promotion_batch_status CHECK (status IN ('CREATED', 'VALIDATED', 'EXECUTED', 'CANCELLED')),
    CONSTRAINT chk_promotion_batch_counts CHECK (promoted_count >= 0 AND repeated_count >= 0 AND graduated_count >= 0 AND total_processed >= 0),
    CONSTRAINT chk_promotion_batch_total CHECK (total_processed >= promoted_count + repeated_count + graduated_count),
    -- On ne promeut pas une année vers elle-même.
    CONSTRAINT chk_promotion_batch_years CHECK (academic_year_id <> next_academic_year_id)
);

-- =============================================================================
-- 4. FINANCE — Frais et paiements (F-10 à F-12)
-- =============================================================================
-- Grille tarifaire de l'école pour une année scolaire.
-- class_id NULL signifie « frais applicable à TOUTES les classes de l'année »
-- (frais général, ex : frais d'inscription identiques pour tous).
--
-- Enum Java associé : FeeType
CREATE TABLE fee_structures(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    class_id UUID REFERENCES classes(id) ON DELETE CASCADE,
    fee_type VARCHAR(20) NOT NULL,
    label VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL, -- montant en GNF
    due_date DATE, -- nullable : frais sans échéance fixe
    installments_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    max_installments SMALLINT DEFAULT 3,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : FeeType
    --   TUITION      : frais de scolarité (le frais principal, souvent échelonné)
    --   REGISTRATION : frais d'inscription
    --   CANTEEN      : cantine
    --   TRANSPORT    : transport scolaire
    --   EXAM         : frais d'examen (BEPC, BAC, examens blancs)
    --   ACTIVITY     : sorties et activités parascolaires
    --   OTHER        : fournitures, tenue scolaire, divers
    CONSTRAINT chk_fee_type CHECK (fee_type IN ('TUITION', 'REGISTRATION', 'CANTEEN', 'TRANSPORT', 'EXAM', 'ACTIVITY', 'OTHER')),
    CONSTRAINT chk_fee_amount CHECK (amount >= 0),
    CONSTRAINT chk_max_installments CHECK (max_installments IS NULL OR max_installments BETWEEN 1 AND 12)
);

-- Unicité de la grille tarifaire.
--
-- Une contrainte UNIQUE (academic_year_id, class_id, fee_type) serait INEFFICACE
-- lorsque class_id vaut NULL, car en SQL NULL <> NULL : plusieurs frais généraux
-- de même type pourraient coexister sans être détectés comme doublons.
--
-- D'où deux index UNIQUE partiels, couvrant chacun un cas de figure.
CREATE UNIQUE INDEX uq_fee_structure_global ON fee_structures(academic_year_id, fee_type)
WHERE
    class_id IS NULL;

CREATE UNIQUE INDEX uq_fee_structure_per_class ON fee_structures(academic_year_id, class_id, fee_type)
WHERE
    class_id IS NOT NULL;

-- Dette individuelle d'un élève sur un frais donné.
--
-- Ces lignes sont générées AUTOMATIQUEMENT à l'inscription :
-- EnrollmentService.enroll() appelle StudentFeeService.generateFeesForEnrollment()
-- qui crée un student_fee par fee_structure applicable (F-09).
--
-- IMPORTANT : les colonnes amount_paid et status sont maintenues par le trigger
-- fn_recalculate_fee_status(). Le code Java ne doit JAMAIS les écrire à la main
-- (à l'exception du statut WAIVED, seule décision manuelle du directeur).
--
-- Enum Java associé : FeeStatus
CREATE TABLE student_fees(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    fee_structure_id UUID NOT NULL REFERENCES fee_structures(id),
    amount_due DECIMAL(19, 2) NOT NULL,
    amount_paid DECIMAL(19, 2) NOT NULL DEFAULT 0, -- calculé par trigger
    discount_amount DECIMAL(19, 2) NOT NULL DEFAULT 0, -- bourse, fratrie, cas social
    discount_reason VARCHAR(255),
    due_date DATE, -- hérité de fee_structure
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID', -- calculé par trigger
    -- Anti-spam des rappels SMS : un parent ne reçoit au maximum qu'un rappel
    -- par semaine et par frais (F-13).
    last_reminder_sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : FeeStatus
    --   UNPAID  : aucun paiement, échéance non dépassée (valeur par défaut)
    --   PARTIAL : paiement partiel, un solde reste dû
    --   PAID    : soldé (remise incluse)
    --   OVERDUE : échéance dépassée et solde non nul
    --             (positionné par le scheduler quotidien, à minuit heure locale)
    --   WAIVED  : exonéré par décision du directeur — SEUL statut posé
    --             manuellement, jamais écrasé par le trigger
    CONSTRAINT chk_student_fee_status CHECK (status IN ('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE', 'WAIVED')),
    -- Empêche les doublons de frais : double-clic du comptable, ou double appel
    -- accidentel de generateFeesForEnrollment().
    CONSTRAINT uq_student_fee UNIQUE (enrollment_id, fee_structure_id),
    CONSTRAINT chk_student_fee_amounts CHECK (amount_due >= 0 AND amount_paid >= 0 AND discount_amount >= 0 AND discount_amount <= amount_due)
);

-- Paiement encaissé par l'école.
--
-- Un paiement n'est JAMAIS supprimé : l'annulation consiste à passer
-- payment_status à CANCELLED et à renseigner les trois colonnes de traçabilité
-- (cancellation_reason, cancelled_by, cancelled_at). Cela garantit une piste
-- d'audit comptable complète.
--
-- Enums Java associés : PaymentMethod, PaymentStatus
CREATE TABLE payments(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id),
    receipt_number VARCHAR(30) UNIQUE, -- REC-YYYYMM-NNNN, généré par trigger
    amount DECIMAL(19, 2) NOT NULL,
    -- Type DATE et non TIMESTAMPTZ : un paiement a une date, pas une heure
    -- précise. Cela évite les décalages de fuseau horaire dans les rapports
    -- journaliers et mensuels (« recettes du 15 novembre »).
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    reference_number VARCHAR(100), -- réf. Orange Money, n° de virement, n° de chèque
    notes TEXT,
    recorded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    -- Traçabilité de l'annulation (F-12).
    cancellation_reason VARCHAR(500),
    cancelled_by UUID REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : PaymentMethod
    --   CASH          : espèces au guichet — méthode dominante en Guinée
    --   ORANGE_MONEY  : Orange Money — premier opérateur mobile money du pays
    --   MTN_MONEY     : MTN Mobile Money
    --   WAVE          : Wave — frais réduits, en forte croissance
    --   BANK_TRANSFER : virement bancaire
    --   CHECK         : chèque
    -- Les opérateurs sont distingués (et non regroupés sous un MOBILE_MONEY
    -- générique) pour permettre au directeur de répondre à la question
    -- « combien avons-nous encaissé via Orange Money ce mois-ci ? ».
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'ORANGE_MONEY', 'MTN_MONEY', 'WAVE', 'BANK_TRANSFER', 'CHECK')),
    -- Enum Java : PaymentStatus
    --   CONFIRMED : encaissé (valeur par défaut)
    --               SEUL statut comptant dans le calcul du solde des frais
    --   CANCELLED : annulé (erreur de saisie, fenêtre de 24h)
    --   REFUNDED  : remboursé au parent (départ de l'élève, trop-perçu)
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('CONFIRMED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    -- Un paiement annulé porte obligatoirement un motif et un horodatage.
    CONSTRAINT chk_payment_cancellation CHECK (payment_status <> 'CANCELLED' OR (cancellation_reason IS NOT NULL AND cancelled_at IS NOT NULL))
);

-- Imputation d'un paiement sur un ou plusieurs frais.
--
-- Un versement unique de 500 000 GNF peut couvrir simultanément la scolarité,
-- la cantine et le transport. La somme des allocations doit être égale au
-- montant du paiement (vérifié par PaymentService, code ALLOCATION_MISMATCH).
CREATE TABLE payment_allocations(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    student_fee_id UUID NOT NULL REFERENCES student_fees(id) ON DELETE CASCADE,
    amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_allocation_amount CHECK (amount > 0),
    -- Un même paiement ne peut pas imputer deux fois le même frais.
    CONSTRAINT uq_payment_allocation UNIQUE (payment_id, student_fee_id)
);

-- =============================================================================
-- 5. COMMUNICATION — SMS (F-13)
-- =============================================================================
-- Modèles de messages. Les variables au format {{nom}} sont résolues à l'envoi
-- par SmsTemplateEngine.
--
-- Enum Java associé : SmsCategory
CREATE TABLE sms_templates(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL, -- fee_reminder, payment_received...
    category VARCHAR(20) NOT NULL,
    content_fr TEXT NOT NULL,
    variables JSONB NOT NULL, -- liste des variables attendues
    -- Permet au directeur de désactiver un modèle sans le supprimer
    -- (ex : suspendre les notifications d'absence pendant les vacances).
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : SmsCategory
    --   FINANCIAL      : relances et confirmations de paiement
    --   ACADEMIC       : bulletins, absences
    --   ADMINISTRATIVE : convocations, informations générales
    --   CUSTOM         : message libre rédigé par le directeur
    CONSTRAINT chk_sms_template_category CHECK (category IN ('FINANCIAL', 'ACADEMIC', 'ADMINISTRATIVE', 'CUSTOM'))
);

-- Journal de TOUS les SMS, y compris les échecs.
-- Sert à la facturation, au débogage et au suivi des relances.
--
-- Enums Java associés : SmsStatus, SmsCategory
CREATE TABLE sms_logs(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID REFERENCES sms_templates(id) ON DELETE SET NULL,
    student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    message TEXT NOT NULL, -- contenu APRÈS résolution des variables
    category VARCHAR(20),
    provider VARCHAR(50), -- orange | twilio | logging
    provider_message_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_code VARCHAR(50),
    error_message TEXT,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : SmsStatus
    --   PENDING   : créé en base, pas encore transmis à l'opérateur (défaut)
    --   SENT      : transmis à l'opérateur et accepté par lui
    --   DELIVERED : accusé de réception confirmé — le SMS est arrivé
    --   FAILED    : échec définitif (error_code et error_message renseignés)
    CONSTRAINT chk_sms_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')),
    -- Enum Java : SmsCategory (mêmes valeurs que sms_templates.category)
    CONSTRAINT chk_sms_log_category CHECK (category IS NULL OR category IN ('FINANCIAL', 'ACADEMIC', 'ADMINISTRATIVE', 'CUSTOM'))
);

-- =============================================================================
-- 6. GRADING — Notes et bulletins (F-15, F-16)
-- =============================================================================
-- Note obtenue par un élève dans une matière, sur un trimestre.
--
-- Enum Java associé : EvaluationType
CREATE TABLE grades(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    class_subject_id UUID NOT NULL REFERENCES class_subjects(id) ON DELETE CASCADE,
    term_id UUID NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
    value DECIMAL(4, 2) NOT NULL, -- note sur 20
    evaluation_type VARCHAR(20) NOT NULL,
    evaluation_label VARCHAR(100) NOT NULL, -- "Devoir 1", "Composition T1"
    evaluation_date DATE NOT NULL,
    entered_by UUID REFERENCES users(id) ON DELETE SET NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : EvaluationType
    --   DEVOIR      : devoir écrit en classe ou à la maison
    --   COMPOSITION : composition trimestrielle (évaluation principale)
    --   ORAL        : interrogation ou exposé oral
    --   TP          : travaux pratiques (sciences, informatique)
    CONSTRAINT chk_evaluation_type CHECK (evaluation_type IN ('DEVOIR', 'COMPOSITION', 'ORAL', 'TP')),
    -- Pas deux « Devoir 1 » en Maths au T1 pour le même élève
    -- (F-15, code erreur DUPLICATE_GRADE).
    CONSTRAINT uq_grade UNIQUE (enrollment_id, class_subject_id, term_id, evaluation_label),
    CONSTRAINT chk_grade_value CHECK (value BETWEEN 0 AND 20)
);

-- Bulletin scolaire d'un élève pour un trimestre.
--
-- Enum Java associé : ReportCardStatus
CREATE TABLE report_cards(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    term_id UUID NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
    general_average DECIMAL(5, 2), -- Σ(moy_matière × coef) / Σ(coef)
    rank_in_class SMALLINT,
    class_size SMALLINT,
    teacher_comment TEXT,
    director_comment TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    pdf_url VARCHAR(255), -- rempli à la publication (génération async)
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : ReportCardStatus
    --   DRAFT          : brouillon — moyennes calculées, appréciations modifiables
    --   PUBLISHED      : PDF généré et figé, consultable par le parent
    --   SENT_TO_PARENT : SMS de notification envoyé — état terminal
    -- Le passage à SENT_TO_PARENT est indispensable : sans lui, impossible de
    -- savoir si le parent a été notifié, et le système renverrait le SMS en boucle.
    CONSTRAINT chk_report_card_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'SENT_TO_PARENT')),
    CONSTRAINT uq_report_card UNIQUE (enrollment_id, term_id),
    CONSTRAINT chk_report_card_average CHECK (general_average IS NULL OR general_average BETWEEN 0 AND 20),
    CONSTRAINT chk_report_card_rank CHECK (rank_in_class IS NULL OR (rank_in_class > 0 AND rank_in_class <= class_size)),
    -- Un bulletin publié possède forcément une moyenne et une date de publication.
    CONSTRAINT chk_report_card_published CHECK (status = 'DRAFT' OR (general_average IS NOT NULL AND published_at IS NOT NULL))
);

-- =============================================================================
-- 7. ATTENDANCE — Présences (F-17)
-- =============================================================================
-- Appel quotidien.
--
-- Enums Java associés : AttendanceStatus, Period
CREATE TABLE attendance(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    period VARCHAR(20) NOT NULL DEFAULT 'FULL_DAY',
    status VARCHAR(20) NOT NULL,
    justification TEXT, -- obligatoire si status = EXCUSED
    recorded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : AttendanceStatus
    --   PRESENT : présent
    --   ABSENT  : absent SANS justification → déclenche le SMS au parent
    --   LATE    : en retard mais présent → pas de SMS
    --   EXCUSED : absence justifiée (le champ justification est renseigné)
    --             → pas de SMS
    --
    -- Le modèle ne retient qu'un seul axe (présence), et non deux axes croisés
    -- (présence × caractère justifié). Une absence ABSENT est régularisable
    -- a posteriori : le directeur la passe en EXCUSED et saisit la justification.
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    -- Enum Java : Period
    --   FULL_DAY  : journée entière (un seul appel par jour) — valeur par défaut
    --   MORNING   : matinée
    --   AFTERNOON : après-midi
    --   EVENING   : cours du soir
    CONSTRAINT chk_attendance_period CHECK (period IN ('FULL_DAY', 'MORNING', 'AFTERNOON', 'EVENING')),
    -- Un seul appel par élève, par jour et par période
    -- (F-17, code erreur ATTENDANCE_ALREADY_RECORDED).
    CONSTRAINT uq_attendance UNIQUE (enrollment_id, DATE, period),
    -- Une absence justifiée porte obligatoirement sa justification.
    CONSTRAINT chk_attendance_excused CHECK (status <> 'EXCUSED' OR justification IS NOT NULL)
);

-- =============================================================================
-- 8. TIMETABLE — Emploi du temps (F-18)
-- =============================================================================
-- Créneau horaire hebdomadaire (ex : lundi 08h00-09h00).
--
-- Enum Java associé : DayOfWeek (enum PROPRE au projet, à ne pas confondre avec
-- java.time.DayOfWeek qui inclut SUNDAY et n'est pas modifiable).
CREATE TABLE time_slots(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    label VARCHAR(30), -- "Heure 1", "Pause déjeuner"
    order_index SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Enum Java : DayOfWeek
    -- Limité à MONDAY..SATURDAY : les écoles guinéennes n'ont pas cours le
    -- dimanche. SUNDAY est volontairement exclu pour empêcher la saisie d'un
    -- créneau aberrant.
    CONSTRAINT chk_day_of_week CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY')),
    CONSTRAINT uq_time_slot UNIQUE (day_of_week, start_time, end_time),
    CONSTRAINT chk_time_slot_times CHECK (end_time > start_time)
);

-- Placement d'une matière dans la grille hebdomadaire.
CREATE TABLE timetable_entries(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_subject_id UUID NOT NULL REFERENCES class_subjects(id) ON DELETE CASCADE,
    time_slot_id UUID NOT NULL REFERENCES time_slots(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    term_id UUID REFERENCES terms(id) ON DELETE CASCADE, -- NULL = toute l'année
    room_number VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_timetable_entry UNIQUE (class_subject_id, time_slot_id, academic_year_id)
    -- NOTE — Détection des conflits d'emploi du temps :
    -- Les deux règles suivantes nécessitent une jointure vers class_subjects et
    -- ne peuvent donc pas être exprimées par une simple contrainte SQL :
    --   • une classe ne peut pas suivre deux matières sur le même créneau
    --     → code erreur CLASS_TIMESLOT_CONFLICT
    --   • un enseignant ne peut pas être à deux endroits en même temps
    --     → code erreur TEACHER_TIMESLOT_CONFLICT
    -- Elles sont vérifiées par TimetableService.validateNoConflict() (F-18).
);

-- =============================================================================
-- 9. INDEX DE PERFORMANCE
--
-- PostgreSQL crée automatiquement un index sur les clés primaires et les
-- contraintes UNIQUE, mais PAS sur les clés étrangères. Les index ci-dessous
-- couvrent les colonnes de jointure et de filtrage les plus sollicitées.
-- =============================================================================
-- ── Identity ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_users_active ON users(is_active);

-- Sert à retrouver, par exemple, tous les utilisateurs actifs ayant un rôle donné.
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- ── Academic ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_terms_year ON terms(academic_year_id);

CREATE INDEX idx_classes_year ON classes(academic_year_id);

CREATE INDEX idx_classes_level ON classes(level_id);

CREATE INDEX idx_class_subjects_class ON class_subjects(class_id);

CREATE INDEX idx_class_subjects_teacher ON class_subjects(teacher_id);

-- ── Enrollment ───────────────────────────────────────────────────────────────
CREATE INDEX idx_students_active ON students(is_active);

CREATE INDEX idx_students_names ON students(last_name, first_name);

-- Sert le scheduler de rappels SMS, qui filtre les élèves ayant un téléphone parent.
CREATE INDEX idx_students_parent_tel ON students(parent_phone);

CREATE INDEX idx_enrollments_student ON enrollments(student_id);

CREATE INDEX idx_enrollments_class ON enrollments(class_id);

CREATE INDEX idx_enrollments_year ON enrollments(academic_year_id);

CREATE INDEX idx_enrollments_status ON enrollments(status);

CREATE INDEX idx_promotion_batch_status ON promotion_batches(status);

CREATE INDEX idx_promotion_batch_year ON promotion_batches(academic_year_id, status);

CREATE INDEX idx_promotion_batch_class ON promotion_batches(class_id, status);

-- ── Finance ──────────────────────────────────────────────────────────────────
CREATE INDEX idx_fee_structures_year ON fee_structures(academic_year_id);

CREATE INDEX idx_fee_structures_class ON fee_structures(class_id);

CREATE INDEX idx_student_fees_enrollment ON student_fees(enrollment_id);

CREATE INDEX idx_student_fees_status ON student_fees(status);

-- Index PARTIEL : sert la requête findFeesNeedingReminder() du scheduler,
-- qui ne s'intéresse qu'aux frais non soldés. L'index ne contient donc que ces
-- lignes, ce qui le rend beaucoup plus compact et rapide.
CREATE INDEX idx_student_fees_reminder ON student_fees(last_reminder_sent_at)
WHERE
    status IN ('UNPAID', 'PARTIAL', 'OVERDUE');

-- Index PARTIEL : sert le scheduler de minuit qui bascule les frais échus
-- en OVERDUE. Seuls les frais encore dus sont concernés.
CREATE INDEX idx_student_fees_due_date ON student_fees(due_date)
WHERE
    status IN ('UNPAID', 'PARTIAL');

CREATE INDEX idx_payments_student ON payments(student_id);

CREATE INDEX idx_payments_date ON payments(payment_date);

CREATE INDEX idx_payments_method ON payments(payment_method);

CREATE INDEX idx_payments_status ON payments(payment_status);

CREATE INDEX idx_payment_alloc_payment ON payment_allocations(payment_id);

CREATE INDEX idx_payment_alloc_fee ON payment_allocations(student_fee_id);

-- ── Grading ──────────────────────────────────────────────────────────────────
CREATE INDEX idx_grades_enrollment_term ON grades(enrollment_id, term_id);

CREATE INDEX idx_grades_class_subject_term ON grades(class_subject_id, term_id);

CREATE INDEX idx_report_cards_term ON report_cards(term_id);

CREATE INDEX idx_report_cards_status ON report_cards(status);

-- ── Attendance ───────────────────────────────────────────────────────────────
CREATE INDEX idx_attendance_enrollment ON attendance(enrollment_id);

CREATE INDEX idx_attendance_date ON attendance(date);

CREATE INDEX idx_attendance_status ON attendance(status);

-- ── Communication ────────────────────────────────────────────────────────────
CREATE INDEX idx_sms_logs_student ON sms_logs(student_id);

CREATE INDEX idx_sms_logs_created_at ON sms_logs(created_at);

CREATE INDEX idx_sms_logs_status ON sms_logs(status);

CREATE INDEX idx_sms_logs_category ON sms_logs(category);

-- ── Timetable ────────────────────────────────────────────────────────────────
CREATE INDEX idx_timetable_class_subject ON timetable_entries(class_subject_id);

CREATE INDEX idx_timetable_time_slot ON timetable_entries(time_slot_id);

CREATE INDEX idx_timetable_year ON timetable_entries(academic_year_id);

-- =============================================================================
-- 10. TRIGGERS updated_at
--
-- Les triggers sont déclarés EXPLICITEMENT, table par table, et non générés par
-- un bloc DO $$ parcourant information_schema.tables.
--
-- Motif : un tel bloc dynamique incluait flyway_schema_history, table qui ne
-- possède pas de colonne updated_at. La création du trigger échouait, et comme
-- le DDL PostgreSQL est transactionnel, TOUTE la migration était annulée. Le
-- schema tenant se retrouvait vide, ne contenant que flyway_schema_history.
--
-- TOUTES les tables sont listées ci-dessous, y compris payments,
-- payment_allocations et sms_logs : leurs entités JPA étendent BaseEntity, qui
-- expose updated_at. Sans la colonne ET son trigger, Hibernate échouerait dès
-- le premier INSERT.
-- =============================================================================
CREATE TRIGGER trg_updated_at_users
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_user_roles
    BEFORE UPDATE ON user_roles
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_teachers
    BEFORE UPDATE ON teachers
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_academic_years
    BEFORE UPDATE ON academic_years
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_terms
    BEFORE UPDATE ON terms
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_levels
    BEFORE UPDATE ON levels
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_classes
    BEFORE UPDATE ON classes
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_subjects
    BEFORE UPDATE ON subjects
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_class_subjects
    BEFORE UPDATE ON class_subjects
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_students
    BEFORE UPDATE ON students
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_enrollments
    BEFORE UPDATE ON enrollments
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_promotion_batches
    BEFORE UPDATE ON promotion_batches
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_fee_structures
    BEFORE UPDATE ON fee_structures
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_student_fees
    BEFORE UPDATE ON student_fees
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_payments
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_payment_allocations
    BEFORE UPDATE ON payment_allocations
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_sms_templates
    BEFORE UPDATE ON sms_templates
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_sms_logs
    BEFORE UPDATE ON sms_logs
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_grades
    BEFORE UPDATE ON grades
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_report_cards
    BEFORE UPDATE ON report_cards
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_attendance
    BEFORE UPDATE ON attendance
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_time_slots
    BEFORE UPDATE ON time_slots
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_timetable_entries
    BEFORE UPDATE ON timetable_entries
    FOR EACH ROW
    EXECUTE FUNCTION fn_update_updated_at();

-- =============================================================================
-- 11. TRIGGER — Génération du matricule élève (EL-YYYY-NNNN)
--
-- pg_advisory_xact_lock sérialise les inscriptions concurrentes.
--
-- Sans ce verrou, deux comptables enregistrant un élève au même instant
-- liraient le même MAX() et généreraient le même matricule. La contrainte
-- UNIQUE en rejetterait un, provoquant une erreur 500 côté utilisateur.
--
-- Le verrou est TRANSACTIONNEL : il se libère automatiquement au COMMIT ou au
-- ROLLBACK, sans risque d'interblocage résiduel.
-- =============================================================================
CREATE OR REPLACE FUNCTION fn_generate_student_number()
    RETURNS TRIGGER
    AS $$
DECLARE
    year_prefix TEXT := TO_CHAR(CURRENT_DATE, 'YYYY');
    next_val INTEGER;
BEGIN
    -- Une seule transaction à la fois peut franchir cette ligne pour une année
    -- donnée ; les autres attendent.
    PERFORM
        pg_advisory_xact_lock(hashtext('student_number_' || year_prefix));
    SELECT
        COALESCE(MAX(SPLIT_PART(student_number, '-', 3)::INTEGER), 0) + 1
    INTO
        next_val
    FROM
        students
    WHERE
        student_number LIKE 'EL-' || year_prefix || '-%';
    NEW.student_number := 'EL-' || year_prefix || '-' || LPAD(next_val::TEXT, 4, '0');
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- Le trigger ne se déclenche que si aucun matricule n'a été fourni,
-- ce qui permet de réimporter des élèves avec leur matricule historique.
CREATE TRIGGER trg_generate_student_number
    BEFORE INSERT ON students
    FOR EACH ROW
    WHEN(NEW.student_number IS NULL)
    EXECUTE FUNCTION fn_generate_student_number();

-- =============================================================================
-- 12. TRIGGER — Génération du numéro de reçu (REC-YYYYMM-NNNN)
--
-- Même protection contre les encaissements simultanés.
-- Le compteur se réinitialise chaque mois (préfixe YYYYMM).
-- =============================================================================
CREATE OR REPLACE FUNCTION fn_generate_receipt_number()
    RETURNS TRIGGER
    AS $$
DECLARE
    month_prefix TEXT := TO_CHAR(CURRENT_DATE, 'YYYYMM');
    next_val INTEGER;
BEGIN
    PERFORM
        pg_advisory_xact_lock(hashtext('receipt_number_' || month_prefix));
    SELECT
        COALESCE(MAX(SPLIT_PART(receipt_number, '-', 3)::INTEGER), 0) + 1
    INTO
        next_val
    FROM
        payments
    WHERE
        receipt_number LIKE 'REC-' || month_prefix || '-%';
    NEW.receipt_number := 'REC-' || month_prefix || '-' || LPAD(next_val::TEXT, 4, '0');
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_receipt_number
    BEFORE INSERT ON payments
    FOR EACH ROW
    WHEN(NEW.receipt_number IS NULL)
    EXECUTE FUNCTION fn_generate_receipt_number();

-- =============================================================================
-- 13. TRIGGER — Recalcul du statut d'un frais
--
-- Recalcule student_fees.amount_paid et student_fees.status après toute
-- modification des imputations. Le code Java ne doit jamais écrire ces colonnes.
--
-- RÈGLE DE CALCUL :
--   restant = amount_due - discount_amount - amount_paid
--     restant <= 0                 → PAID
--     amount_paid > 0              → PARTIAL
--     échéance dépassée            → OVERDUE
--     sinon                        → UNPAID
--
-- Deux bugs corrigés dans cette version :
--
--   1. NEW est NULL lors d'un DELETE.
--      L'ancienne version lisait NEW.student_fee_id sans garde : supprimer une
--      imputation provoquait une erreur.
--      → COALESCE(NEW.student_fee_id, OLD.student_fee_id)
--
--   2. Les paiements annulés étaient comptés comme encaissés.
--      La somme portait sur TOUTES les payment_allocations, sans regarder le
--      statut du paiement parent. Un paiement passé à CANCELLED continuait donc
--      d'être compté : le frais restait PAID alors que l'argent n'avait jamais
--      été perçu.
--      → jointure sur payments, filtrée sur payment_status = 'CONFIRMED'
-- =============================================================================
CREATE OR REPLACE FUNCTION fn_recalculate_fee_status()
    RETURNS TRIGGER
    AS $$
DECLARE
    v_fee_id UUID;
    v_total_paid DECIMAL(19, 2);
    v_amount_due DECIMAL(19, 2);
    v_discount DECIMAL(19, 2);
    v_due_date DATE;
    v_status VARCHAR(20);
    v_remaining DECIMAL(19, 2);
BEGIN
    -- Fonctionne indifféremment pour INSERT, UPDATE et DELETE.
    v_fee_id := COALESCE(NEW.student_fee_id, OLD.student_fee_id);
    -- Seules les imputations issues d'un paiement CONFIRMED sont comptabilisées.
    SELECT
        COALESCE(SUM(pa.amount), 0)
    INTO
        v_total_paid
    FROM
        payment_allocations pa
        JOIN payments p ON p.id = pa.payment_id
    WHERE
        pa.student_fee_id = v_fee_id
        AND p.payment_status = 'CONFIRMED';
    SELECT
        amount_due,
        discount_amount,
        due_date,
        status
    INTO
        v_amount_due,
        v_discount,
        v_due_date,
        v_status
    FROM
        student_fees
    WHERE
        id = v_fee_id;
    -- WAIVED est une décision manuelle du directeur (exonération) :
    -- ne jamais l'écraser automatiquement.
    IF v_status = 'WAIVED' THEN
        RETURN COALESCE(NEW, OLD);
    END IF;
    v_remaining := v_amount_due - v_discount - v_total_paid;
    UPDATE
        student_fees
    SET
        amount_paid = v_total_paid,
        status = CASE WHEN v_remaining <= 0 THEN
            'PAID'
        WHEN v_total_paid > 0 THEN
            'PARTIAL'
        WHEN v_due_date IS NOT NULL
            AND v_due_date < CURRENT_DATE THEN
            'OVERDUE'
        ELSE
            'UNPAID'
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE
        id = v_fee_id;
    RETURN COALESCE(NEW, OLD);
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_recalculate_fee_status
    AFTER INSERT OR UPDATE OR DELETE ON payment_allocations
    FOR EACH ROW
    EXECUTE FUNCTION fn_recalculate_fee_status();

-- =============================================================================
-- 14. TRIGGER — Répercussion d'un changement de statut de paiement
--
-- Sans ce trigger, annuler un paiement (CONFIRMED → CANCELLED) ne mettait à
-- jour AUCUN frais : les student_fees imputés par ce paiement restaient PAID,
-- alors que l'argent n'était plus considéré comme encaissé.
--
-- Ici, tout changement de payment_status force le recalcul de chacun des frais
-- imputés par le paiement concerné.
-- =============================================================================
CREATE OR REPLACE FUNCTION fn_payment_status_changed()
    RETURNS TRIGGER
    AS $$
DECLARE
    v_fee_id UUID;
    v_total_paid DECIMAL(19, 2);
    v_amount_due DECIMAL(19, 2);
    v_discount DECIMAL(19, 2);
    v_due_date DATE;
    v_status VARCHAR(20);
    v_remaining DECIMAL(19, 2);
BEGIN
    -- Rien à faire si le statut n'a pas changé.
    IF NEW.payment_status IS NOT DISTINCT FROM OLD.payment_status THEN
        RETURN NEW;
    END IF;
    -- Recalculer chaque frais touché par ce paiement.
    FOR v_fee_id IN
    SELECT
        student_fee_id
    FROM
        payment_allocations
    WHERE
        payment_id = NEW.id LOOP
            SELECT
                COALESCE(SUM(pa.amount), 0)
            INTO
                v_total_paid
            FROM
                payment_allocations pa
                JOIN payments p ON p.id = pa.payment_id
            WHERE
                pa.student_fee_id = v_fee_id
                AND p.payment_status = 'CONFIRMED';
            SELECT
                amount_due,
                discount_amount,
                due_date,
                status
            INTO
                v_amount_due,
                v_discount,
                v_due_date,
                v_status
            FROM
                student_fees
            WHERE
                id = v_fee_id;
            -- Ne jamais écraser une exonération.
            CONTINUE
            WHEN v_status = 'WAIVED';
            v_remaining := v_amount_due - v_discount - v_total_paid;
            UPDATE
                student_fees
            SET
                amount_paid = v_total_paid,
                status = CASE WHEN v_remaining <= 0 THEN
                    'PAID'
                WHEN v_total_paid > 0 THEN
                    'PARTIAL'
                WHEN v_due_date IS NOT NULL
                    AND v_due_date < CURRENT_DATE THEN
                    'OVERDUE'
                ELSE
                    'UNPAID'
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE
                id = v_fee_id;
        END LOOP;
    RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_status_changed
    AFTER UPDATE OF payment_status ON payments
    FOR EACH ROW
    EXECUTE FUNCTION fn_payment_status_changed();

-- =============================================================================
-- 15. VUE MATÉRIALISÉE — Statistiques du tableau de bord (F-14)
--
-- Une vue matérialisée stocke physiquement le résultat de la requête, ce qui
-- rend la lecture instantanée. En contrepartie, les données ne se mettent pas à
-- jour toutes seules : DashboardScheduler exécute un REFRESH toutes les 15 min.
--
-- Créée WITH NO DATA : au moment de la migration les tables sont vides, il est
-- inutile de calculer. Le premier REFRESH peuplera la vue.
--
-- ATTENTION — REFRESH MATERIALIZED VIEW CONCURRENTLY échoue tant que la vue n'a
-- jamais été peuplée. DashboardService doit donc gérer ce premier appel avec un
-- REFRESH classique (sans CONCURRENTLY) en repli.
--
-- ATTENTION — La vue vit dans le schema TENANT. Comme JdbcTemplate ne passe pas
-- par le MultiTenantConnectionProvider d'Hibernate, le TenantContext n'a aucun
-- effet sur le search_path : le nom du schema doit être qualifié explicitement
-- dans la requête de REFRESH.
-- =============================================================================
CREATE MATERIALIZED VIEW mv_dashboard_stats AS
SELECT
    -- Effectifs
    COALESCE(COUNT(DISTINCT e.id) FILTER (WHERE e.status = 'ENROLLED'), 0) AS active_students,
    COALESCE(COUNT(DISTINCT e.student_id), 0) AS total_students,
    COALESCE(COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender = 'M'), 0) AS male_students,
    COALESCE(COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender = 'F'), 0) AS female_students,
    -- Finances
    COALESCE(SUM(sf.amount_due - sf.discount_amount), 0) AS total_fees_expected,
    COALESCE(SUM(sf.amount_paid), 0) AS total_fees_collected,
    CASE WHEN COALESCE(SUM(sf.amount_due - sf.discount_amount), 0) > 0 THEN
        ROUND(100.0 * COALESCE(SUM(sf.amount_paid), 0) / SUM(sf.amount_due - sf.discount_amount), 1)
    ELSE
        0
    END AS collection_rate_pct,
    COALESCE(COUNT(DISTINCT e.id) FILTER (WHERE sf.status IN ('UNPAID', 'PARTIAL', 'OVERDUE')), 0) AS students_with_debt,
    COALESCE(COUNT(DISTINCT e.id) FILTER (WHERE sf.status = 'OVERDUE'), 0) AS students_overdue,
    -- Saisies de notes en attente : nombre de couples (matière × trimestre
    -- ouvert) pour lesquels aucune note n'a encore été saisie.
    --
    -- CORRECTION : l'ancienne version comptait « grades WHERE value IS NULL »,
    -- ce qui renvoyait toujours 0 puisqu'une note sans valeur ne peut pas
    -- exister (colonne NOT NULL). Le compteur était donc inutile.
(
        SELECT
            COUNT(*)
        FROM
            class_subjects cs
        CROSS JOIN terms t
    WHERE
        t.grades_entry_open = TRUE
        AND NOT EXISTS (
            SELECT
                1
            FROM
                grades g
            WHERE
                g.class_subject_id = cs.id
                AND g.term_id = t.id)) AS pending_grade_entries,
    -- Volumétrie SMS
(
        SELECT
            COUNT(*)
    FROM sms_logs
    WHERE
        DATE(created_at) = CURRENT_DATE) AS sms_today,
(
        SELECT
            COUNT(*)
        FROM
            sms_logs
        WHERE
            created_at >= DATE_TRUNC('month', CURRENT_TIMESTAMP)) AS sms_this_month
FROM
    enrollments e
    JOIN students s ON e.student_id = s.id
    LEFT JOIN student_fees sf ON sf.enrollment_id = e.id
WHERE
    e.academic_year_id IN (
        SELECT
            id
        FROM
            academic_years
        WHERE
            is_current = TRUE
        LIMIT 1
)
WITH NO DATA;

-- Index UNIQUE obligatoire pour autoriser REFRESH ... CONCURRENTLY.
-- La vue ne renvoyant qu'une seule ligne (des agrégats globaux), on indexe sur
-- la constante 1 : cela satisfait l'exigence de PostgreSQL sans nécessiter de
-- colonne réellement unique.
CREATE UNIQUE INDEX idx_mv_dashboard_stats ON mv_dashboard_stats((1));

-- =============================================================================
-- 16. DONNÉES INITIALES
-- =============================================================================
-- Niveaux du système scolaire guinéen.
-- order_index définit la progression du cursus, utilisée par la promotion (F-19).
INSERT INTO levels(name, order_index)
VALUES
    ('Primaire', 1),
('Collège', 2),
('Lycée', 3);

-- Modèles de SMS (F-13).
-- Les variables {{…}} sont remplacées à l'envoi par SmsTemplateEngine.
-- NOTE : en SQL, une apostrophe à l'intérieur d'une chaîne se double ('').
INSERT INTO sms_templates(code, category, content_fr, variables)
VALUES
    ('fee_reminder', 'FINANCIAL', 'Bonjour {{parent_name}}, les frais de {{student_name}} ({{class_name}}) s''élèvent à {{amount_due}} GNF. Merci de régler avant le {{due_date}}.', '["parent_name","student_name","class_name","amount_due","due_date"]'::JSONB),
('payment_received', 'FINANCIAL', 'Bonjour {{parent_name}}, paiement de {{amount_paid}} GNF reçu pour {{student_name}}. Reste dû: {{remaining}} GNF. Reçu n°{{receipt_number}}.', '["parent_name","student_name","amount_paid","remaining","receipt_number"]'::JSONB),
('report_card_published', 'ACADEMIC', 'Bonjour {{parent_name}}, le bulletin de {{term_name}} de {{student_name}} est disponible. Moy: {{average}}/20. Rang: {{rank}}/{{class_size}}.', '["parent_name","term_name","student_name","average","rank","class_size"]'::JSONB),
('absence_notification', 'ACADEMIC', 'Bonjour {{parent_name}}, votre enfant {{student_name}} était absent(e) le {{date}}. Merci de nous contacter.', '["parent_name","student_name","date"]'::JSONB);


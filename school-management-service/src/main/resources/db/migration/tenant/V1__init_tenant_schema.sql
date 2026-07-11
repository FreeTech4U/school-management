-- ============================================================================
-- V1__init_tenant_schema.sql
-- Schema TENANT — Données métier d'UNE école
--
-- IMPORTANT : ce script s'exécute avec search_path = tenant_xxx UNIQUEMENT.
--             Le schema "public" n'est PAS dans le search_path.
--             → Toute fonction utilisée ici DOIT être définie ici.
-- ============================================================================

-- ── Fonction updated_at ─────────────────────────────────────────────────────
-- CORRECTION : redéfinie dans le schema tenant (elle existait uniquement dans
--              public → introuvable au moment de créer les triggers → rollback)

CREATE OR REPLACE FUNCTION fn_update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- 1. IDENTITY — Utilisateurs et enseignants
-- ============================================================================

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    avatar_url    VARCHAR(255),
    is_active     BOOLEAN DEFAULT TRUE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_user_role CHECK (role IN ('DIRECTOR', 'TEACHER', 'ACCOUNTANT', 'PARENT'))
);

CREATE TABLE teachers (
    id              UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    employee_number VARCHAR(50) UNIQUE,
    hire_date       DATE,
    specialty       VARCHAR(100),
    qualification   VARCHAR(255),
    bio             TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);


-- ============================================================================
-- 2. ACADEMIC — Structure pédagogique
-- ============================================================================

CREATE TABLE academic_years (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    label      VARCHAR(50) UNIQUE NOT NULL,
    start_date DATE NOT NULL,
    end_date   DATE NOT NULL,
    is_current BOOLEAN DEFAULT FALSE NOT NULL,
    status     VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_year_status CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT chk_year_dates  CHECK (end_date > start_date)
);

CREATE TABLE terms (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id  UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    name              VARCHAR(100) NOT NULL,
    term_number       SMALLINT NOT NULL,
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL,
    is_current        BOOLEAN DEFAULT FALSE NOT NULL,
    grades_entry_open BOOLEAN DEFAULT FALSE NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_term_number_per_year UNIQUE (academic_year_id, term_number),
    CONSTRAINT chk_term_number         CHECK (term_number BETWEEN 1 AND 4),
    CONSTRAINT chk_term_dates          CHECK (end_date > start_date)
);

CREATE TABLE levels (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) UNIQUE NOT NULL,
    order_index SMALLINT UNIQUE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE classes (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    level_id         UUID NOT NULL REFERENCES levels(id),
    name             VARCHAR(100) NOT NULL,
    option           VARCHAR(100),
    capacity         SMALLINT,
    room_number      VARCHAR(20),
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_class_name_per_year UNIQUE (academic_year_id, name),
    CONSTRAINT chk_class_capacity     CHECK (capacity IS NULL OR capacity > 0)
);

CREATE TABLE subjects (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(100) UNIQUE NOT NULL,
    code       VARCHAR(20) UNIQUE,
    color      VARCHAR(7),
    is_active  BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_subject_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE class_subjects (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    class_id     UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    subject_id   UUID NOT NULL REFERENCES subjects(id),
    teacher_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    coefficient  SMALLINT DEFAULT 1 NOT NULL,
    weekly_hours SMALLINT,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_class_subject UNIQUE (class_id, subject_id),
    CONSTRAINT chk_coefficient   CHECK (coefficient BETWEEN 1 AND 10),
    CONSTRAINT chk_weekly_hours CHECK (weekly_hours IS NULL OR weekly_hours > 0)
);


-- ============================================================================
-- 3. ENROLLMENT — Élèves et inscriptions
-- ============================================================================

CREATE TABLE students (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_number VARCHAR(20) UNIQUE,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    -- CORRECTION : date_of_birth et gender rendus nullable
    --              (inscriptions rapides sans documents complets)
    date_of_birth  DATE,
    gender         CHAR(1),
    birth_city     VARCHAR(100),
    birth_country  VARCHAR(3) DEFAULT 'GN',
    photo_url      VARCHAR(255),
    address        TEXT,
    parent_name    VARCHAR(255),
    parent_phone   VARCHAR(20),
    medical_notes  TEXT,
    is_active      BOOLEAN DEFAULT TRUE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_student_gender CHECK (gender IS NULL OR gender IN ('M', 'F'))
);

CREATE TABLE enrollments (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id             UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    class_id               UUID NOT NULL REFERENCES classes(id),
    academic_year_id       UUID NOT NULL REFERENCES academic_years(id),
    enrollment_date        DATE DEFAULT CURRENT_DATE NOT NULL,
    is_repeating           BOOLEAN DEFAULT FALSE NOT NULL,
    status                 VARCHAR(20) DEFAULT 'ENROLLED' NOT NULL,
    promotion_status       VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    -- CORRECTION : traçabilité de la validation des promotions
    promotion_validated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    promotion_validated_at TIMESTAMP WITH TIME ZONE,
    final_average          DECIMAL(5, 2),
    notes                  TEXT,
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_enrollment_per_year UNIQUE (student_id, academic_year_id),
    CONSTRAINT chk_enrollment_status  CHECK (status IN ('ENROLLED', 'TRANSFERRED', 'WITHDRAWN', 'GRADUATED')),
    CONSTRAINT chk_promotion_status   CHECK (promotion_status IN ('PENDING', 'PROMOTED', 'REPEATED', 'GRADUATED')),
    CONSTRAINT chk_final_average      CHECK (final_average IS NULL OR final_average BETWEEN 0 AND 20)
);


-- ============================================================================
-- 4. FINANCE — Frais et paiements
-- ============================================================================

CREATE TABLE fee_structures (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id     UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    class_id             UUID REFERENCES classes(id) ON DELETE CASCADE,
    fee_type             VARCHAR(50) NOT NULL,
    label                VARCHAR(255) NOT NULL,
    amount               DECIMAL(19, 2) NOT NULL,
    -- CORRECTION : due_date rendu nullable (certains frais sans échéance fixe)
    due_date             DATE,
    installments_allowed BOOLEAN DEFAULT FALSE NOT NULL,
    max_installments     SMALLINT DEFAULT 3,
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_fee_type         CHECK (fee_type IN ('TUITION', 'REGISTRATION', 'CANTEEN', 'TRANSPORT', 'EXAM', 'UNIFORM', 'OTHER')),
    CONSTRAINT chk_fee_amount       CHECK (amount >= 0),
    CONSTRAINT chk_max_installments CHECK (max_installments IS NULL OR max_installments BETWEEN 1 AND 12)
);

-- CORRECTION : UNIQUE(academic_year_id, class_id, fee_type) ne fonctionne pas
--              quand class_id IS NULL (NULL != NULL en SQL).
--              → 2 index partiels pour couvrir les deux cas.

CREATE UNIQUE INDEX uq_fee_structure_global
    ON fee_structures (academic_year_id, fee_type)
    WHERE class_id IS NULL;

CREATE UNIQUE INDEX uq_fee_structure_per_class
    ON fee_structures (academic_year_id, class_id, fee_type)
    WHERE class_id IS NOT NULL;


CREATE TABLE student_fees (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id         UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    fee_structure_id      UUID NOT NULL REFERENCES fee_structures(id),
    amount_due            DECIMAL(19, 2) NOT NULL,
    amount_paid           DECIMAL(19, 2) DEFAULT 0 NOT NULL,
    discount_amount       DECIMAL(19, 2) DEFAULT 0 NOT NULL,
    discount_reason       VARCHAR(255),
    -- CORRECTION : due_date nullable (hérité de fee_structure)
    due_date              DATE,
    status                VARCHAR(20) DEFAULT 'UNPAID' NOT NULL,
    last_reminder_sent_at TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- CORRECTION : empêche les doublons de frais (double-clic, appel deux fois
    --              de generateFeesForEnrollment())
    CONSTRAINT uq_student_fee UNIQUE (enrollment_id, fee_structure_id),

    CONSTRAINT chk_student_fee_status  CHECK (status IN ('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE', 'WAIVED')),
    CONSTRAINT chk_student_fee_amounts CHECK (
        amount_due >= 0 AND amount_paid >= 0 AND discount_amount >= 0
            AND discount_amount <= amount_due
        )
);


CREATE TABLE payments (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id       UUID NOT NULL REFERENCES students(id),
    receipt_number   VARCHAR(30) UNIQUE,
    amount           DECIMAL(19, 2) NOT NULL,
    -- CORRECTION : DATE au lieu de TIMESTAMP (un paiement a une date, pas une heure)
    payment_date     DATE DEFAULT CURRENT_DATE NOT NULL,
    payment_method   VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    notes            TEXT,
    recorded_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('cash', 'orange_money', 'mtn_money', 'wave', 'bank_transfer', 'check'))
);


CREATE TABLE payment_allocations (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id     UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    student_fee_id UUID NOT NULL REFERENCES student_fees(id) ON DELETE CASCADE,
    amount         DECIMAL(19, 2) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_allocation_amount CHECK (amount > 0)
);


-- ============================================================================
-- 5. COMMUNICATION — SMS
-- ============================================================================

CREATE TABLE sms_templates (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code       VARCHAR(50) UNIQUE NOT NULL,
    category   VARCHAR(20) NOT NULL,
    content_fr TEXT NOT NULL,
    variables  JSONB,
    -- CORRECTION : permet de désactiver un template sans le supprimer
    is_active  BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_sms_template_category CHECK (category IN ('FINANCIAL', 'ACADEMIC', 'ADMINISTRATIVE', 'CUSTOM'))
);

CREATE TABLE sms_logs (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- CORRECTION : traçabilité du template utilisé
    template_id         UUID REFERENCES sms_templates(id) ON DELETE SET NULL,
    student_id          UUID REFERENCES students(id) ON DELETE SET NULL,
    recipient_phone     VARCHAR(20) NOT NULL,
    message             TEXT NOT NULL,
    -- CORRECTION : catégorie pour filtrer/reporter les SMS
    category            VARCHAR(20),
    provider            VARCHAR(50),
    provider_message_id VARCHAR(100),
    status              VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    error_code          VARCHAR(50),
    error_message       TEXT,
    sent_at             TIMESTAMP WITH TIME ZONE,
    delivered_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_sms_log_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED'))
);


-- ============================================================================
-- 6. GRADING — Notes et bulletins
-- ============================================================================

CREATE TABLE grades (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id    UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    class_subject_id UUID NOT NULL REFERENCES class_subjects(id) ON DELETE CASCADE,
    term_id          UUID NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
    value            DECIMAL(4, 2),
    evaluation_type  VARCHAR(20) NOT NULL,
    evaluation_label VARCHAR(100) NOT NULL,
    evaluation_date  DATE,
    entered_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    comment          TEXT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_grade        UNIQUE (enrollment_id, class_subject_id, term_id, evaluation_label),
    CONSTRAINT chk_grade_value CHECK (value IS NULL OR value BETWEEN 0 AND 20),
    CONSTRAINT chk_grade_type  CHECK (evaluation_type IN ('DEVOIR', 'COMPOSITION', 'ORAL', 'TP'))
);

CREATE TABLE report_cards (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id    UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    term_id          UUID NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
    general_average  DECIMAL(5, 2),
    rank_in_class    SMALLINT,
    class_size       SMALLINT,
    teacher_comment  TEXT,
    director_comment TEXT,
    status           VARCHAR(20) DEFAULT 'DRAFT' NOT NULL,
    pdf_url          VARCHAR(255),
    published_at     TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_report_card          UNIQUE (enrollment_id, term_id),
    CONSTRAINT chk_report_card_status  CHECK (status IN ('DRAFT', 'PUBLISHED', 'SENT_TO_PARENT')),
    CONSTRAINT chk_report_card_average CHECK (general_average IS NULL OR general_average BETWEEN 0 AND 20),
    CONSTRAINT chk_report_card_rank    CHECK (rank_in_class IS NULL OR rank_in_class > 0)
);


-- ============================================================================
-- 7. ATTENDANCE — Présences
-- ============================================================================

CREATE TABLE attendance (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    date          DATE NOT NULL,
    period        VARCHAR(20) DEFAULT 'FULL_DAY' NOT NULL,
    status        VARCHAR(20) NOT NULL,
    justification TEXT,
    recorded_by   UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_attendance         UNIQUE (enrollment_id, date, period),
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    CONSTRAINT chk_attendance_period CHECK (period IN ('FULL_DAY', 'MORNING', 'AFTERNOON'))
);


-- ============================================================================
-- 8. TIMETABLE — Emploi du temps
-- ============================================================================

CREATE TABLE time_slots (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    day_of_week VARCHAR(10) NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    label       VARCHAR(30),
    order_index SMALLINT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_time_slot         UNIQUE (day_of_week, start_time, end_time),
    CONSTRAINT chk_day_of_week      CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT chk_time_slot_times CHECK (end_time > start_time)
);

CREATE TABLE timetable_entries (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    class_subject_id UUID NOT NULL REFERENCES class_subjects(id) ON DELETE CASCADE,
    time_slot_id     UUID NOT NULL REFERENCES time_slots(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    term_id          UUID REFERENCES terms(id) ON DELETE CASCADE,
    room_number      VARCHAR(20),
    is_active        BOOLEAN DEFAULT TRUE NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- CORRECTION : empêche de placer 2x la même matière sur le même créneau
    CONSTRAINT uq_timetable_entry UNIQUE (class_subject_id, time_slot_id, academic_year_id)
);


-- ============================================================================
-- INDEX — Performance
-- CORRECTION : aucun index n'existait au-delà des PK/FK
-- ============================================================================

-- Identity
CREATE INDEX idx_users_role ON users (role) WHERE is_active = TRUE;
CREATE INDEX idx_users_active ON users (is_active);

-- Academic
CREATE INDEX idx_academic_years_current ON academic_years (is_current) WHERE is_current = TRUE;
CREATE INDEX idx_terms_year ON terms (academic_year_id);
CREATE INDEX idx_terms_current ON terms (is_current) WHERE is_current = TRUE;
CREATE INDEX idx_classes_year ON classes (academic_year_id);
CREATE INDEX idx_classes_level ON classes (level_id);
CREATE INDEX idx_class_subjects_class ON class_subjects (class_id);
CREATE INDEX idx_class_subjects_teacher ON class_subjects (teacher_id);

-- Enrollment
CREATE INDEX idx_students_active ON students (is_active);
CREATE INDEX idx_students_names ON students (last_name, first_name);
CREATE INDEX idx_enrollments_student ON enrollments (student_id);
CREATE INDEX idx_enrollments_class ON enrollments (class_id);
CREATE INDEX idx_enrollments_year ON enrollments (academic_year_id);
CREATE INDEX idx_enrollments_status ON enrollments (status);

-- Finance
CREATE INDEX idx_fee_structures_year ON fee_structures (academic_year_id);
CREATE INDEX idx_fee_structures_class ON fee_structures (class_id);
CREATE INDEX idx_student_fees_enrollment ON student_fees (enrollment_id);
CREATE INDEX idx_student_fees_status ON student_fees (status);
CREATE INDEX idx_student_fees_due_date ON student_fees (due_date)
    WHERE status IN ('UNPAID', 'PARTIAL', 'OVERDUE');
CREATE INDEX idx_payments_student ON payments (student_id);
CREATE INDEX idx_payments_date ON payments (payment_date);
CREATE INDEX idx_payments_method ON payments (payment_method);
CREATE INDEX idx_payment_allocations_pay ON payment_allocations (payment_id);
CREATE INDEX idx_payment_allocations_fee ON payment_allocations (student_fee_id);

-- Grading
CREATE INDEX idx_grades_enrollment_term ON grades (enrollment_id, term_id);
CREATE INDEX idx_grades_class_subject_term ON grades (class_subject_id, term_id);
CREATE INDEX idx_report_cards_term ON report_cards (term_id);
CREATE INDEX idx_report_cards_status ON report_cards (status);

-- Attendance
CREATE INDEX idx_attendance_enrollment ON attendance (enrollment_id);
CREATE INDEX idx_attendance_date ON attendance (date);
CREATE INDEX idx_attendance_status ON attendance (status);

-- Communication
CREATE INDEX idx_sms_logs_student ON sms_logs (student_id);
CREATE INDEX idx_sms_logs_created_at ON sms_logs (created_at);
CREATE INDEX idx_sms_logs_status ON sms_logs (status);
CREATE INDEX idx_sms_logs_category ON sms_logs (category);

-- Timetable
CREATE INDEX idx_timetable_class_subject ON timetable_entries (class_subject_id);
CREATE INDEX idx_timetable_time_slot ON timetable_entries (time_slot_id);
CREATE INDEX idx_timetable_year ON timetable_entries (academic_year_id);


-- ============================================================================
-- TRIGGERS updated_at
-- CORRECTION : liste explicite au lieu d'un DO $$ dynamique
--              (le DO block incluait flyway_schema_history qui n'a pas
--               de colonne updated_at → erreur → rollback complet)
-- ============================================================================

CREATE TRIGGER trg_updated_at_users
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_teachers
    BEFORE UPDATE ON teachers FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_academic_years
    BEFORE UPDATE ON academic_years FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_terms
    BEFORE UPDATE ON terms FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_levels
    BEFORE UPDATE ON levels FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_classes
    BEFORE UPDATE ON classes FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_subjects
    BEFORE UPDATE ON subjects FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_class_subjects
    BEFORE UPDATE ON class_subjects FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_students
    BEFORE UPDATE ON students FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_enrollments
    BEFORE UPDATE ON enrollments FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_fee_structures
    BEFORE UPDATE ON fee_structures FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_student_fees
    BEFORE UPDATE ON student_fees FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_sms_templates
    BEFORE UPDATE ON sms_templates FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_grades
    BEFORE UPDATE ON grades FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_report_cards
    BEFORE UPDATE ON report_cards FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_attendance
    BEFORE UPDATE ON attendance FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_time_slots
    BEFORE UPDATE ON time_slots FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TRIGGER trg_updated_at_timetable_entries
    BEFORE UPDATE ON timetable_entries FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();


-- ============================================================================
-- TRIGGER — Matricule élève : EL-YYYY-NNNN
-- CORRECTION : COUNT(*)+1 avait une race condition en cas d'inscriptions
--              simultanées → deux élèves avec le même matricule → violation
--              de contrainte UNIQUE.
--              pg_advisory_xact_lock sérialise les inserts concurrents.
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_generate_student_number()
    RETURNS TRIGGER AS $$
DECLARE
    year_prefix TEXT := TO_CHAR(CURRENT_DATE, 'YYYY');
    next_val    INTEGER;
BEGIN
    -- Verrou transactionnel : les inserts concurrents attendent le COMMIT
    PERFORM pg_advisory_xact_lock(hashtext('student_number_' || year_prefix));

    SELECT COALESCE(MAX(CAST(SPLIT_PART(student_number, '-', 3) AS INTEGER)), 0) + 1
    INTO next_val
    FROM students
    WHERE student_number LIKE 'EL-' || year_prefix || '-%';

    NEW.student_number := 'EL-' || year_prefix || '-' || LPAD(next_val::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_student_number
    BEFORE INSERT ON students
    FOR EACH ROW
    WHEN (NEW.student_number IS NULL)
EXECUTE FUNCTION fn_generate_student_number();


-- ============================================================================
-- TRIGGER — Numéro de reçu : REC-YYYYMM-NNNN
-- CORRECTION : même race condition que le matricule élève.
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_generate_receipt_number()
    RETURNS TRIGGER AS $$
DECLARE
    month_prefix TEXT := TO_CHAR(CURRENT_DATE, 'YYYYMM');
    next_val     INTEGER;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('receipt_number_' || month_prefix));

    SELECT COALESCE(MAX(CAST(SPLIT_PART(receipt_number, '-', 3) AS INTEGER)), 0) + 1
    INTO next_val
    FROM payments
    WHERE receipt_number LIKE 'REC-' || month_prefix || '-%';

    NEW.receipt_number := 'REC-' || month_prefix || '-' || LPAD(next_val::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_receipt_number
    BEFORE INSERT ON payments
    FOR EACH ROW
    WHEN (NEW.receipt_number IS NULL)
EXECUTE FUNCTION fn_generate_receipt_number();


-- ============================================================================
-- TRIGGER — Recalcul du statut des frais
-- CORRECTION : NEW est NULL sur DELETE → il fallait COALESCE(NEW, OLD)
--              sinon la suppression d'une allocation plantait.
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_recalculate_fee_status()
    RETURNS TRIGGER AS $$
DECLARE
    v_fee_id     UUID;
    v_total_paid DECIMAL(19, 2);
    v_amount_due DECIMAL(19, 2);
    v_discount   DECIMAL(19, 2);
    v_due_date   DATE;
    v_status     VARCHAR(20);
    v_remaining  DECIMAL(19, 2);
BEGIN
    -- Fonctionne pour INSERT, UPDATE et DELETE
    v_fee_id := COALESCE(NEW.student_fee_id, OLD.student_fee_id);

    -- Total effectivement payé sur ce frais
    SELECT COALESCE(SUM(amount), 0)
    INTO v_total_paid
    FROM payment_allocations
    WHERE student_fee_id = v_fee_id;

    -- Données du frais
    SELECT amount_due, discount_amount, due_date, status
    INTO v_amount_due, v_discount, v_due_date, v_status
    FROM student_fees
    WHERE id = v_fee_id;

    -- Ne jamais écraser un statut WAIVED (exonération décidée manuellement)
    IF v_status = 'WAIVED' THEN
        RETURN COALESCE(NEW, OLD);
    END IF;

    v_remaining := v_amount_due - v_discount - v_total_paid;

    UPDATE student_fees
    SET amount_paid = v_total_paid,
        status      = CASE
                          WHEN v_remaining <= 0                                     THEN 'PAID'
                          WHEN v_total_paid > 0                                     THEN 'PARTIAL'
                          WHEN v_due_date IS NOT NULL AND v_due_date < CURRENT_DATE THEN 'OVERDUE'
                          ELSE 'UNPAID'
            END,
        updated_at  = CURRENT_TIMESTAMP
    WHERE id = v_fee_id;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recalculate_fee_status
    AFTER INSERT OR UPDATE OR DELETE ON payment_allocations
    FOR EACH ROW
EXECUTE FUNCTION fn_recalculate_fee_status();


-- ============================================================================
-- VUE MATÉRIALISÉE — Dashboard
-- Rafraîchie toutes les 15 min par le scheduler
-- ============================================================================

CREATE MATERIALIZED VIEW mv_dashboard_stats AS
SELECT COALESCE(COUNT(DISTINCT e.id) FILTER (WHERE e.status = 'ENROLLED'), 0)  AS active_students,
       COALESCE(COUNT(DISTINCT e.student_id), 0)                                AS total_students,
       COALESCE(COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender = 'M'), 0) AS male_students,
       COALESCE(COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender = 'F'), 0) AS female_students,
       COALESCE(SUM(sf.amount_due - sf.discount_amount), 0)                    AS total_fees_expected,
       COALESCE(SUM(sf.amount_paid), 0)                                        AS total_fees_collected,
       CASE
           WHEN COALESCE(SUM(sf.amount_due - sf.discount_amount), 0) > 0
               THEN ROUND(100.0 * COALESCE(SUM(sf.amount_paid), 0)
                              / SUM(sf.amount_due - sf.discount_amount), 1)
           ELSE 0
           END                                                                  AS collection_rate_pct,
       COALESCE(COUNT(DISTINCT e.id) FILTER (
           WHERE sf.status IN ('UNPAID', 'PARTIAL', 'OVERDUE')), 0)            AS students_with_debt,
       COALESCE(COUNT(DISTINCT e.id) FILTER (WHERE sf.status = 'OVERDUE'), 0)  AS students_overdue,
       (SELECT COUNT(*) FROM grades WHERE value IS NULL)                        AS pending_grade_entries,
       (SELECT COUNT(*) FROM sms_logs WHERE DATE(created_at) = CURRENT_DATE)   AS sms_today,
       (SELECT COUNT(*)
        FROM sms_logs
        WHERE created_at >= DATE_TRUNC('month', CURRENT_TIMESTAMP))            AS sms_this_month
FROM enrollments e
         JOIN students s ON e.student_id = s.id
         LEFT JOIN student_fees sf ON sf.enrollment_id = e.id
WHERE e.academic_year_id IN (
    SELECT id FROM academic_years WHERE is_current = TRUE LIMIT 1
)
WITH NO DATA;

-- Index UNIQUE requis pour REFRESH MATERIALIZED VIEW CONCURRENTLY
CREATE UNIQUE INDEX idx_mv_dashboard_stats ON mv_dashboard_stats ((1));


-- ============================================================================
-- DONNÉES INITIALES
-- ============================================================================

-- Niveaux scolaires (Guinée)
INSERT INTO levels (name, order_index)
VALUES ('Primaire', 1),
       ('Collège', 2),
       ('Lycée', 3);

-- Templates SMS
INSERT INTO sms_templates (code, category, content_fr, variables)
VALUES ('fee_reminder', 'FINANCIAL',
        'Bonjour {{parent_name}}, les frais de {{student_name}} ({{class_name}}) s''élèvent à {{amount_due}} GNF. Merci de régler avant le {{due_date}}.',
        '["parent_name","student_name","class_name","amount_due","due_date"]'::jsonb),

       ('payment_received', 'FINANCIAL',
        'Bonjour {{parent_name}}, paiement de {{amount_paid}} GNF reçu pour {{student_name}}. Reste dû: {{remaining}} GNF. Reçu n°{{receipt_number}}.',
        '["parent_name","student_name","amount_paid","remaining","receipt_number"]'::jsonb),

       ('report_card_published', 'ACADEMIC',
        'Bonjour {{parent_name}}, le bulletin de {{term_name}} de {{student_name}} est disponible. Moy: {{average}}/20. Rang: {{rank}}/{{class_size}}.',
        '["parent_name","term_name","student_name","average","rank","class_size"]'::jsonb),

       ('absence_notification', 'ACADEMIC',
        'Bonjour {{parent_name}}, votre enfant {{student_name}} était absent(e) le {{date}}. Merci de nous contacter.',
        '["parent_name","student_name","date"]'::jsonb);

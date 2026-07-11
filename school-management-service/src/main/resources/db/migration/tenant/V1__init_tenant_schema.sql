-- 1. Identity
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- DIRECTOR, TEACHER, ACCOUNTANT, PARENT
    avatar_url VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teachers (
    id UUID PRIMARY KEY REFERENCES users(id),
    employee_number VARCHAR(50) UNIQUE,
    hire_date DATE,
    specialty VARCHAR(100),
    qualification VARCHAR(255),
    bio TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Academic
CREATE TABLE academic_years (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    label VARCHAR(50) UNIQUE NOT NULL, -- 2024-2025
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, CLOSED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE terms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    name VARCHAR(100) NOT NULL, -- 1er Trimestre
    term_number INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    grades_entry_open BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(academic_year_id, term_number)
);

CREATE TABLE levels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL, -- Primaire, Collège, Lycée
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE classes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    level_id UUID NOT NULL REFERENCES levels(id),
    name VARCHAR(100) NOT NULL, -- 6ème A
    option VARCHAR(100),
    capacity INTEGER,
    room_number VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(academic_year_id, name)
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    code VARCHAR(20) UNIQUE,
    color VARCHAR(7),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE class_subjects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    class_id UUID NOT NULL REFERENCES classes(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    teacher_id UUID REFERENCES users(id),
    coefficient INTEGER DEFAULT 1,
    weekly_hours INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(class_id, subject_id)
);

-- 3. Enrollment
CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_number VARCHAR(20) UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender CHAR(1) NOT NULL,
    birth_city VARCHAR(100),
    birth_country VARCHAR(100) DEFAULT 'GN',
    photo_url VARCHAR(255),
    address TEXT,
    parent_name VARCHAR(255),
    parent_phone VARCHAR(50),
    medical_notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES students(id),
    class_id UUID NOT NULL REFERENCES classes(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    enrollment_date DATE DEFAULT CURRENT_DATE,
    is_repeating BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ENROLLED', -- ENROLLED, TRANSFERRED, WITHDRAWN, GRADUATED
    promotion_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROMOTED, REPEATED, GRADUATED
    final_average DECIMAL(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, academic_year_id)
);

-- 4. Finance
CREATE TABLE fee_structures (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    class_id UUID REFERENCES classes(id), -- NULL = all classes
    fee_type VARCHAR(50) NOT NULL, -- TUITION, REGISTRATION, CANTEEN, TRANSPORT, EXAM
    label VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    due_date DATE NOT NULL,
    installments_allowed BOOLEAN DEFAULT FALSE,
    max_installments INTEGER DEFAULT 3,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(academic_year_id, class_id, fee_type)
);

CREATE TABLE student_fees (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id),
    fee_structure_id UUID NOT NULL REFERENCES fee_structures(id),
    amount_due DECIMAL(19, 2) NOT NULL,
    amount_paid DECIMAL(19, 2) DEFAULT 0,
    discount_amount DECIMAL(19, 2) DEFAULT 0,
    discount_reason VARCHAR(255),
    due_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'UNPAID', -- UNPAID, PARTIAL, PAID, OVERDUE, WAIVED
    last_reminder_sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES students(id),
    receipt_number VARCHAR(50) UNIQUE,
    amount DECIMAL(19, 2) NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    notes TEXT,
    recorded_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_allocations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID NOT NULL REFERENCES payments(id),
    student_fee_id UUID NOT NULL REFERENCES student_fees(id),
    amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Communication
CREATE TABLE sms_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    category VARCHAR(20) NOT NULL, -- FINANCIAL, ACADEMIC
    content_fr TEXT NOT NULL,
    variables JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sms_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID REFERENCES students(id),
    recipient_phone VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    provider VARCHAR(50),
    provider_message_id VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    error_code VARCHAR(50),
    error_message TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Grading
CREATE TABLE grades (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id),
    class_subject_id UUID NOT NULL REFERENCES class_subjects(id),
    term_id UUID NOT NULL REFERENCES terms(id),
    value DECIMAL(4, 2),
    evaluation_type VARCHAR(20) NOT NULL, -- DEVOIR, COMPOSITION, ORAL, TP
    evaluation_label VARCHAR(100) NOT NULL,
    evaluation_date DATE NOT NULL,
    entered_by UUID REFERENCES users(id),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(enrollment_id, class_subject_id, term_id, evaluation_label)
);

CREATE TABLE report_cards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id),
    term_id UUID NOT NULL REFERENCES terms(id),
    general_average DECIMAL(4, 2),
    rank_in_class INTEGER,
    class_size INTEGER,
    teacher_comment TEXT,
    director_comment TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, SENT_TO_PARENT
    pdf_url VARCHAR(255),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(enrollment_id, term_id)
);

-- 7. Attendance
CREATE TABLE attendance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES enrollments(id),
    date DATE NOT NULL,
    period VARCHAR(20) DEFAULT 'FULL_DAY', -- FULL_DAY, MORNING, AFTERNOON
    status VARCHAR(20) NOT NULL, -- PRESENT, ABSENT, LATE, EXCUSED
    justification TEXT,
    recorded_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(enrollment_id, date, period)
);

-- 8. Timetable
CREATE TABLE time_slots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    label VARCHAR(50),
    order_index INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE timetable_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    class_subject_id UUID NOT NULL REFERENCES class_subjects(id),
    time_slot_id UUID NOT NULL REFERENCES time_slots(id),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term_id UUID REFERENCES terms(id), -- NULL = whole year
    room_number VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Triggers

-- Student Number Generator: EL-YYYY-NNNN
CREATE OR REPLACE FUNCTION fn_generate_student_number() RETURNS TRIGGER AS $$
DECLARE
    year_prefix TEXT := TO_CHAR(CURRENT_DATE, 'YYYY');
    next_val INTEGER;
BEGIN
    SELECT COUNT(*) + 1 INTO next_val FROM students WHERE student_number LIKE 'EL-' || year_prefix || '-%';
    NEW.student_number := 'EL-' || year_prefix || '-' || LPAD(next_val::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_student_number
BEFORE INSERT ON students
FOR EACH ROW
WHEN (NEW.student_number IS NULL)
EXECUTE FUNCTION fn_generate_student_number();

-- Receipt Number Generator: REC-YYYYMM-NNNN
CREATE OR REPLACE FUNCTION fn_generate_receipt_number() RETURNS TRIGGER AS $$
DECLARE
    month_prefix TEXT := TO_CHAR(CURRENT_DATE, 'YYYYMM');
    next_val INTEGER;
BEGIN
    SELECT COUNT(*) + 1 INTO next_val FROM payments WHERE receipt_number LIKE 'REC-' || month_prefix || '-%';
    NEW.receipt_number := 'REC-' || month_prefix || '-' || LPAD(next_val::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_receipt_number
BEFORE INSERT ON payments
FOR EACH ROW
WHEN (NEW.receipt_number IS NULL)
EXECUTE FUNCTION fn_generate_receipt_number();

-- Recalculate StudentFee amount_paid and status
CREATE OR REPLACE FUNCTION fn_recalculate_fee_status() RETURNS TRIGGER AS $$
BEGIN
    UPDATE student_fees
    SET amount_paid = (SELECT COALESCE(SUM(amount), 0) FROM payment_allocations WHERE student_fee_id = NEW.student_fee_id),
        status = CASE 
            WHEN (amount_due - discount_amount - (SELECT COALESCE(SUM(amount), 0) FROM payment_allocations WHERE student_fee_id = NEW.student_fee_id)) <= 0 THEN 'PAID'
            WHEN (SELECT COALESCE(SUM(amount), 0) FROM payment_allocations WHERE student_fee_id = NEW.student_fee_id) > 0 THEN 'PARTIAL'
            WHEN due_date < CURRENT_DATE THEN 'OVERDUE'
            ELSE 'UNPAID'
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.student_fee_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recalculate_fee_status
AFTER INSERT OR UPDATE OR DELETE ON payment_allocations
FOR EACH ROW
EXECUTE FUNCTION fn_recalculate_fee_status();

-- Updated_at triggers for all tables
DO $$ 
DECLARE 
    t TEXT;
BEGIN
    FOR t IN 
        SELECT table_name FROM information_schema.tables 
        WHERE table_schema = current_schema() 
        AND table_type = 'BASE TABLE' 
        AND table_name NOT IN ('payment_allocations', 'payments', 'sms_logs') -- exclude tables without updated_at
    LOOP
        EXECUTE format('CREATE TRIGGER trg_update_updated_at_%I BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at()', t, t);
    END LOOP;
END $$;

-- 10. Dashboard Materialized View
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_dashboard_stats AS
SELECT
    COUNT(DISTINCT e.id) FILTER (WHERE e.status = 'ENROLLED') AS active_students,
    COUNT(DISTINCT e.student_id)                               AS total_students,
    COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender='M')  AS male_students,
    COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender='F')  AS female_students,
    COALESCE(SUM(sf.amount_due - sf.discount_amount), 0)       AS total_fees_expected,
    COALESCE(SUM(sf.amount_paid), 0)                           AS total_fees_collected,
    CASE WHEN SUM(sf.amount_due - sf.discount_amount)>0
         THEN ROUND(100.0 * SUM(sf.amount_paid)/SUM(sf.amount_due-sf.discount_amount),1)
         ELSE 0 END                                            AS collection_rate_pct,
    COUNT(DISTINCT e.id) FILTER (WHERE sf.status IN ('UNPAID','PARTIAL','OVERDUE')) AS students_with_debt,
    COUNT(DISTINCT e.id) FILTER (WHERE sf.status = 'OVERDUE') AS students_overdue,
    (SELECT COUNT(*) FROM grades WHERE value IS NULL)          AS pending_grade_entries,
    (SELECT COUNT(*) FROM sms_logs WHERE DATE(created_at) = CURRENT_DATE) AS sms_today,
    (SELECT COUNT(*) FROM sms_logs WHERE created_at >= DATE_TRUNC('month',NOW())) AS sms_this_month
FROM enrollments e
JOIN students s ON e.student_id = s.id
LEFT JOIN student_fees sf ON sf.enrollment_id = e.id
    WHERE e.academic_year_id = (SELECT id FROM academic_years WHERE is_current=TRUE LIMIT 1)
WITH NO DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_dashboard_stats_id ON mv_dashboard_stats((1));

-- Seed data for SMS templates
INSERT INTO sms_templates (code, category, content_fr, variables) VALUES
('fee_reminder', 'FINANCIAL',
 'Bonjour {{parent_name}}, les frais de {{student_name}} ({{class_name}}) s''élèvent à {{amount_due}} GNF. Merci de régler avant le {{due_date}}.',
 '["parent_name","student_name","class_name","amount_due","due_date"]'),

('payment_received', 'FINANCIAL',
 'Bonjour {{parent_name}}, paiement de {{amount_paid}} GNF reçu pour {{student_name}}. Reste dû: {{remaining}} GNF. Reçu n°{{receipt_number}}.',
 '["parent_name","student_name","amount_paid","remaining","receipt_number"]'),

('report_card_published', 'ACADEMIC',
 'Bonjour {{parent_name}}, le bulletin de {{term_name}} de {{student_name}} est disponible. Moy: {{average}}/20. Rang: {{rank}}/{{class_size}}.',
 '["parent_name","term_name","student_name","average","rank","class_size"]'),

('absence_notification', 'ACADEMIC',
 'Bonjour {{parent_name}}, votre enfant {{student_name}} était absent(e) le {{date}}. Merci de nous contacter.',
 '["parent_name","student_name","date"]');

-- Seed data for Levels
INSERT INTO levels (name, order_index) VALUES ('Primaire', 1), ('Collège', 2), ('Lycée', 3);

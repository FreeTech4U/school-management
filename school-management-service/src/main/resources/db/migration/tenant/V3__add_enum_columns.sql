-- ============================================================================
-- V3__add_enum_columns.sql
-- Convert legacy VARCHAR business columns from V1 to PostgreSQL ENUM types
-- created in V2, while preserving defaults and remapping incompatible values.
-- ============================================================================

-- Dashboard materialized view --------------------------------------------------
-- PostgreSQL refuses ALTER COLUMN TYPE when a materialized view rule depends on
-- the column. Drop the view first and recreate it after all enum conversions.
DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_stats;

-- Dependent indexes ------------------------------------------------------------
-- Partial indexes that reference legacy VARCHAR status values must be dropped
-- before changing the column type, then recreated against the enum type.
DROP INDEX IF EXISTS idx_student_fees_due_date;

-- Payments -------------------------------------------------------------------
ALTER TABLE payments
  DROP CONSTRAINT IF EXISTS chk_payment_method;

ALTER TABLE payments
  ALTER COLUMN payment_method TYPE payment_method
  USING CASE lower(payment_method)
    WHEN 'cash' THEN 'CASH'::payment_method
    WHEN 'orange_money' THEN 'MOBILE_MONEY'::payment_method
    WHEN 'mtn_money' THEN 'MOBILE_MONEY'::payment_method
    WHEN 'wave' THEN 'MOBILE_MONEY'::payment_method
    WHEN 'bank_transfer' THEN 'BANK_TRANSFER'::payment_method
    WHEN 'check' THEN 'CHECK'::payment_method
    ELSE 'CASH'::payment_method
  END,
  ADD COLUMN payment_status payment_status NOT NULL DEFAULT 'CONFIRMED';

-- Student fees ----------------------------------------------------------------
ALTER TABLE student_fees
  DROP CONSTRAINT IF EXISTS chk_student_fee_status;

ALTER TABLE student_fees
  ALTER COLUMN status DROP DEFAULT;

ALTER TABLE student_fees
  ALTER COLUMN status TYPE fee_status
  USING CASE status
    WHEN 'UNPAID' THEN 'UNPAID'::fee_status
    WHEN 'PARTIAL' THEN 'PARTIAL'::fee_status
    WHEN 'PAID' THEN 'PAID'::fee_status
    WHEN 'OVERDUE' THEN 'OVERDUE'::fee_status
    WHEN 'WAIVED' THEN 'WAIVED'::fee_status
    ELSE 'UNPAID'::fee_status
  END;

ALTER TABLE student_fees
  ALTER COLUMN status SET DEFAULT 'UNPAID';

-- Fee structures --------------------------------------------------------------
ALTER TABLE fee_structures
  DROP CONSTRAINT IF EXISTS chk_fee_type;

ALTER TABLE fee_structures
  ALTER COLUMN fee_type TYPE fee_type
  USING CASE fee_type
    WHEN 'TUITION' THEN 'TUITION'::fee_type
    WHEN 'REGISTRATION' THEN 'REGISTRATION'::fee_type
    WHEN 'CANTEEN' THEN 'CANTEEN'::fee_type
    WHEN 'TRANSPORT' THEN 'TRANSPORT'::fee_type
    WHEN 'EXAM' THEN 'EXAM'::fee_type
    WHEN 'UNIFORM' THEN 'OTHER'::fee_type
    WHEN 'OTHER' THEN 'OTHER'::fee_type
    ELSE 'OTHER'::fee_type
  END;

-- Enrollments -----------------------------------------------------------------
ALTER TABLE enrollments
  DROP CONSTRAINT IF EXISTS chk_enrollment_status,
  DROP CONSTRAINT IF EXISTS chk_promotion_status;

ALTER TABLE enrollments
  ALTER COLUMN status DROP DEFAULT,
  ALTER COLUMN promotion_status DROP DEFAULT;

ALTER TABLE enrollments
  ALTER COLUMN status TYPE enrollment_status
  USING CASE status
    WHEN 'ENROLLED' THEN 'ACTIVE'::enrollment_status
    WHEN 'TRANSFERRED' THEN 'TRANSFERRED'::enrollment_status
    WHEN 'WITHDRAWN' THEN 'DROPPED_OUT'::enrollment_status
    WHEN 'GRADUATED' THEN 'GRADUATED'::enrollment_status
    ELSE 'ACTIVE'::enrollment_status
  END,
  ALTER COLUMN promotion_status TYPE promotion_status
  USING CASE promotion_status
    WHEN 'PENDING' THEN 'PENDING'::promotion_status
    WHEN 'PROMOTED' THEN 'PROMOTED'::promotion_status
    WHEN 'REPEATED' THEN 'RETAINED'::promotion_status
    WHEN 'GRADUATED' THEN 'PROMOTED'::promotion_status
    ELSE 'PENDING'::promotion_status
  END,
  ADD COLUMN transfer_notes VARCHAR(500);

ALTER TABLE enrollments
  ALTER COLUMN status SET DEFAULT 'ACTIVE',
  ALTER COLUMN promotion_status SET DEFAULT 'PENDING';

-- Grades ----------------------------------------------------------------------
ALTER TABLE grades
  DROP CONSTRAINT IF EXISTS chk_grade_type;

ALTER TABLE grades
  ALTER COLUMN evaluation_type TYPE evaluation_type
  USING CASE evaluation_type
    WHEN 'DEVOIR' THEN 'EXAM'::evaluation_type
    WHEN 'COMPOSITION' THEN 'CONTINUOUS_ASSESSMENT'::evaluation_type
    WHEN 'ORAL' THEN 'PARTICIPATION'::evaluation_type
    WHEN 'TP' THEN 'PRACTICAL'::evaluation_type
    ELSE 'EXAM'::evaluation_type
  END;

-- Attendance ------------------------------------------------------------------
ALTER TABLE attendance
  DROP CONSTRAINT IF EXISTS chk_attendance_period,
  DROP CONSTRAINT IF EXISTS chk_attendance_status;

ALTER TABLE attendance
  ALTER COLUMN period DROP DEFAULT;

ALTER TABLE attendance
  ALTER COLUMN period TYPE period
  USING CASE period
    WHEN 'MORNING' THEN 'MORNING'::period
    WHEN 'AFTERNOON' THEN 'AFTERNOON'::period
    WHEN 'FULL_DAY' THEN 'FULL_DAY'::period
    ELSE 'FULL_DAY'::period
  END,
  ALTER COLUMN status TYPE attendance_status
  USING CASE status
    WHEN 'PRESENT' THEN 'PRESENT'::attendance_status
    WHEN 'ABSENT' THEN 'ABSENT'::attendance_status
    WHEN 'LATE' THEN 'LATE'::attendance_status
    WHEN 'EXCUSED' THEN 'EXCUSED'::attendance_status
    ELSE 'ABSENT'::attendance_status
  END;

ALTER TABLE attendance
  ALTER COLUMN period SET DEFAULT 'FULL_DAY';

-- Report cards ----------------------------------------------------------------
ALTER TABLE report_cards
  DROP CONSTRAINT IF EXISTS chk_report_card_status;

ALTER TABLE report_cards
  ALTER COLUMN status DROP DEFAULT;

ALTER TABLE report_cards
  ALTER COLUMN status TYPE report_card_status
  USING CASE status
    WHEN 'DRAFT' THEN 'DRAFT'::report_card_status
    WHEN 'PUBLISHED' THEN 'PUBLISHED'::report_card_status
    WHEN 'SENT_TO_PARENT' THEN 'PUBLISHED'::report_card_status
    ELSE 'DRAFT'::report_card_status
  END;

ALTER TABLE report_cards
  ALTER COLUMN status SET DEFAULT 'DRAFT';

-- SMS logs --------------------------------------------------------------------
ALTER TABLE sms_logs
  DROP CONSTRAINT IF EXISTS chk_sms_log_status;

ALTER TABLE sms_logs
  ALTER COLUMN status DROP DEFAULT;

ALTER TABLE sms_logs
  ALTER COLUMN status TYPE sms_status
  USING CASE status
    WHEN 'PENDING' THEN 'PENDING'::sms_status
    WHEN 'SENT' THEN 'SENT'::sms_status
    WHEN 'DELIVERED' THEN 'DELIVERED'::sms_status
    WHEN 'FAILED' THEN 'FAILED'::sms_status
    ELSE 'PENDING'::sms_status
  END;

ALTER TABLE sms_logs
  ALTER COLUMN status SET DEFAULT 'PENDING';

-- Dashboard materialized view --------------------------------------------------
CREATE MATERIALIZED VIEW mv_dashboard_stats AS
SELECT COALESCE(COUNT(DISTINCT e.id) FILTER (WHERE e.status = 'ACTIVE'), 0)     AS active_students,
       COALESCE(COUNT(DISTINCT e.student_id), 0)                                 AS total_students,
       COALESCE(COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender = 'M'), 0)  AS male_students,
       COALESCE(COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender = 'F'), 0)  AS female_students,
       COALESCE(SUM(sf.amount_due - sf.discount_amount), 0)                      AS total_fees_expected,
       COALESCE(SUM(sf.amount_paid), 0)                                          AS total_fees_collected,
       CASE
           WHEN COALESCE(SUM(sf.amount_due - sf.discount_amount), 0) > 0
               THEN ROUND(100.0 * COALESCE(SUM(sf.amount_paid), 0)
                              / SUM(sf.amount_due - sf.discount_amount), 1)
           ELSE 0
           END                                                                   AS collection_rate_pct,
       COALESCE(COUNT(DISTINCT e.id) FILTER (
           WHERE sf.status IN ('UNPAID', 'PARTIAL', 'OVERDUE')), 0)             AS students_with_debt,
       COALESCE(COUNT(DISTINCT e.id) FILTER (WHERE sf.status = 'OVERDUE'), 0)   AS students_overdue,
       (SELECT COUNT(*) FROM grades WHERE value IS NULL)                         AS pending_grade_entries,
       (SELECT COUNT(*) FROM sms_logs WHERE DATE(created_at) = CURRENT_DATE)     AS sms_today,
       (SELECT COUNT(*)
        FROM sms_logs
        WHERE created_at >= DATE_TRUNC('month', CURRENT_TIMESTAMP))              AS sms_this_month
FROM enrollments e
         JOIN students s ON e.student_id = s.id
         LEFT JOIN student_fees sf ON sf.enrollment_id = e.id
WHERE e.academic_year_id IN (
    SELECT id FROM academic_years WHERE is_current = TRUE LIMIT 1
)
WITH NO DATA;

CREATE UNIQUE INDEX idx_mv_dashboard_stats ON mv_dashboard_stats ((1));

CREATE INDEX idx_student_fees_due_date ON student_fees (due_date)
    WHERE status IN ('UNPAID', 'PARTIAL', 'OVERDUE');

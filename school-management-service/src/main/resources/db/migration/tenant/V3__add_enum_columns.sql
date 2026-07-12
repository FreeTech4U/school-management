-- ============================================================================
-- V3__add_enum_columns.sql
-- Convert String columns to PostgreSQL ENUM types and add missing columns
-- ============================================================================

-- Payment table: convert payment_method to enum and add status column
ALTER TABLE payments 
  ALTER COLUMN payment_method TYPE payment_method USING payment_method::payment_method,
  ADD COLUMN payment_status payment_status NOT NULL DEFAULT 'CONFIRMED';

-- StudentFee table: convert status to enum
ALTER TABLE student_fees 
  ALTER COLUMN status TYPE fee_status USING status::fee_status;

-- FeeStructure table: convert feeType to enum
ALTER TABLE fee_structures 
  ALTER COLUMN fee_type TYPE fee_type USING fee_type::fee_type;

-- StudentEnrollment table: convert status to enum and add transfer_notes
ALTER TABLE enrollments 
  ALTER COLUMN status TYPE enrollment_status USING status::enrollment_status,
  ALTER COLUMN promotion_status TYPE promotion_status USING promotion_status::promotion_status,
  ADD COLUMN transfer_notes VARCHAR(500);

-- Grade table: convert evaluationType to enum
ALTER TABLE grades 
  ALTER COLUMN evaluation_type TYPE evaluation_type USING evaluation_type::evaluation_type;

-- Attendance table: convert period and status to enum
ALTER TABLE attendance 
  ALTER COLUMN period TYPE period USING period::period,
  ALTER COLUMN status TYPE attendance_status USING status::attendance_status;

-- ReportCard table: convert status to enum
ALTER TABLE report_cards 
  ALTER COLUMN status TYPE report_card_status USING status::report_card_status;

-- SmsLog table: convert status to enum
ALTER TABLE sms_logs 
  ALTER COLUMN status TYPE sms_status USING status::sms_status;

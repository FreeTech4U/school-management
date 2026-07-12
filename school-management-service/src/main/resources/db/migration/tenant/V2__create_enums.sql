-- ============================================================================
-- V2__create_enums.sql
-- Creates PostgreSQL ENUM types for all business domain enums
-- ============================================================================

-- Enrollment domain
CREATE TYPE enrollment_status AS ENUM ('ACTIVE', 'INACTIVE', 'TRANSFERRED', 'GRADUATED', 'DROPPED_OUT', 'SUSPENDED');
CREATE TYPE promotion_status AS ENUM ('PROMOTED', 'RETAINED', 'CONDITIONAL', 'PENDING', 'OVERRIDDEN');

-- Finance domain
CREATE TYPE fee_status AS ENUM ('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE', 'WAIVED', 'EXEMPTED');
CREATE TYPE fee_type AS ENUM ('TUITION', 'REGISTRATION', 'CANTEEN', 'TRANSPORT', 'EXAM', 'ACTIVITY', 'OTHER');
CREATE TYPE payment_method AS ENUM ('CASH', 'BANK_TRANSFER', 'CHECK', 'CREDIT_CARD', 'MOBILE_MONEY', 'WIRE_TRANSFER', 'CRYPTO');
CREATE TYPE payment_status AS ENUM ('PENDING', 'CONFIRMED', 'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED');

-- Grading domain
CREATE TYPE evaluation_type AS ENUM ('EXAM', 'CONTINUOUS_ASSESSMENT', 'ASSIGNMENT', 'PROJECT', 'PARTICIPATION', 'PRACTICAL', 'QUIZ');
CREATE TYPE report_card_status AS ENUM ('DRAFT', 'GENERATED', 'PUBLISHED', 'ARCHIVED', 'CORRECTED');

-- Attendance domain
CREATE TYPE period AS ENUM ('MORNING', 'AFTERNOON', 'EVENING', 'FULL_DAY');
CREATE TYPE attendance_status AS ENUM ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED', 'JUSTIFIED', 'ABSENT_UNJUSTIFIED');

-- Communication domain
CREATE TYPE sms_status AS ENUM ('QUEUED', 'SENT', 'DELIVERED', 'FAILED', 'PENDING', 'BOUNCED', 'OPTED_OUT');

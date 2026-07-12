-- V4__add_class_subject_validations.sql
-- Add validations and constraints to ClassSubject entity
-- Implements Phase 2: Add coefficient + weekly_hours constraints

-- Step 1: Add unique constraint on (class_id, subject_id) for ClassSubject
-- This prevents duplicate subject assignments to the same class
ALTER TABLE class_subjects
ADD CONSTRAINT uk_class_subject UNIQUE (class_id, subject_id);

-- Step 2: Add CHECK constraints for coefficient (1-10 range per specs)
ALTER TABLE class_subjects
ADD CONSTRAINT ck_coefficient_range CHECK (coefficient BETWEEN 1 AND 10);

-- Step 3: Add CHECK constraint for weekly_hours if not null (1-50 range)
ALTER TABLE class_subjects
ADD CONSTRAINT ck_weekly_hours_range CHECK (weekly_hours IS NULL OR (weekly_hours BETWEEN 1 AND 50));

-- Step 4: Set default coefficient value for any existing NULL values
UPDATE class_subjects 
SET coefficient = 1 
WHERE coefficient IS NULL;

-- Step 5: Make coefficient NOT NULL after setting defaults
ALTER TABLE class_subjects
ALTER COLUMN coefficient SET NOT NULL;
ALTER TABLE class_subjects
ALTER COLUMN coefficient SET DEFAULT 1;

-- Step 6: Create index on (class_id, subject_id) for fast lookups
CREATE INDEX idx_class_subject_unique ON class_subjects(class_id, subject_id);

-- Step 7: Create index on teacher_id for efficient teacher subject queries
CREATE INDEX idx_class_subject_teacher ON class_subjects(teacher_id);

-- Verify: Show updated schema
-- \d class_subjects;

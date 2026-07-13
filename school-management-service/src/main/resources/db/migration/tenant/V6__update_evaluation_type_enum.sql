-- ============================================================================
-- V6__update_evaluation_type_enum.sql
-- Updates EvaluationType enum to match specifications (DEVOIR, COMPOSITION, ORAL, TP)
-- ============================================================================

-- Update evaluation_type enum: Create new type and migrate data
ALTER TYPE evaluation_type RENAME TO evaluation_type_old;

CREATE TYPE evaluation_type AS ENUM ('DEVOIR', 'COMPOSITION', 'ORAL', 'TP');

-- Migrate existing grades table to use new enum
ALTER TABLE grades 
    ALTER COLUMN evaluation_type TYPE evaluation_type 
    USING CASE 
        WHEN evaluation_type::text = 'EXAM' THEN 'DEVOIR'::evaluation_type
        WHEN evaluation_type::text = 'CONTINUOUS_ASSESSMENT' THEN 'COMPOSITION'::evaluation_type
        WHEN evaluation_type::text = 'ASSIGNMENT' THEN 'DEVOIR'::evaluation_type
        WHEN evaluation_type::text = 'PROJECT' THEN 'TP'::evaluation_type
        WHEN evaluation_type::text = 'PARTICIPATION' THEN 'ORAL'::evaluation_type
        WHEN evaluation_type::text = 'PRACTICAL' THEN 'TP'::evaluation_type
        WHEN evaluation_type::text = 'QUIZ' THEN 'COMPOSITION'::evaluation_type
        ELSE 'DEVOIR'::evaluation_type
    END;

DROP TYPE evaluation_type_old;

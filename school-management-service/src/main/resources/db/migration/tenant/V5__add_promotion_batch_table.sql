-- V5__add_promotion_batch_table.sql
-- Create PromotionBatch table for managing bulk student promotions

CREATE TABLE promotion_batches (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id      UUID NOT NULL,
    next_academic_year_id UUID NOT NULL,
    class_id              UUID NOT NULL,
    status                VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    promoted_count        INTEGER DEFAULT 0,
    repeated_count        INTEGER DEFAULT 0,
    graduated_count       INTEGER DEFAULT 0,
    total_processed       INTEGER DEFAULT 0,
    validation_errors     TEXT,
    executed_at           TIMESTAMP WITH TIME ZONE,
    notes                 TEXT,
    director_comment      TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_promotion_batch_academic_year FOREIGN KEY (academic_year_id)
        REFERENCES academic_years(id) ON DELETE RESTRICT,
    CONSTRAINT fk_promotion_batch_next_year FOREIGN KEY (next_academic_year_id)
        REFERENCES academic_years(id) ON DELETE RESTRICT,
    CONSTRAINT fk_promotion_batch_class FOREIGN KEY (class_id)
        REFERENCES classes(id) ON DELETE RESTRICT,
    CONSTRAINT ck_promotion_batch_status CHECK (status IN ('CREATED', 'VALIDATED', 'EXECUTED', 'CANCELLED')),
    CONSTRAINT ck_promotion_batch_counts CHECK (
        promoted_count >= 0
        AND repeated_count >= 0
        AND graduated_count >= 0
        AND total_processed >= 0
    ),
    CONSTRAINT ck_promotion_batch_total CHECK (
        total_processed >= (promoted_count + repeated_count + graduated_count)
    )
);

CREATE INDEX idx_promotion_batch_status ON promotion_batches(status);
CREATE INDEX idx_promotion_batch_academic_year ON promotion_batches(academic_year_id, status);
CREATE INDEX idx_promotion_batch_class ON promotion_batches(class_id, status);
CREATE INDEX idx_promotion_batch_created_at ON promotion_batches(created_at DESC);

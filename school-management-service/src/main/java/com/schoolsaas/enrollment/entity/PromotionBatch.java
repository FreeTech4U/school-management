package com.schoolsaas.enrollment.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a batch promotion operation for students.
 * Tracks promotion of students from one academic year to the next,
 * or repetition in the same class.
 */
@Entity
@Table(name = "promotion_batches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionBatch extends BaseEntity {

    /**
     * Current academic year being promoted from
     */
    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    /**
     * Next academic year for promotion
     */
    @Column(name = "next_academic_year_id", nullable = false)
    private UUID nextAcademicYearId;

    /**
     * The class being promoted
     */
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    /**
     * Status: CREATED -> VALIDATED -> EXECUTED
     */
    @Column(nullable = false)
    private String status = "CREATED"; // CREATED, VALIDATED, EXECUTED, CANCELLED

    /**
     * Count of promoted students (promotion to next class)
     */
    @Column(name = "promoted_count")
    private Integer promotedCount = 0;

    /**
     * Count of repeated students (repeat same class)
     */
    @Column(name = "repeated_count")
    private Integer repeatedCount = 0;

    /**
     * Count of graduated students (last class of level)
     */
    @Column(name = "graduated_count")
    private Integer graduatedCount = 0;

    /**
     * Total count of enrollments processed
     */
    @Column(name = "total_processed")
    private Integer totalProcessed = 0;

    /**
     * Validation errors, if any
     */
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    /**
     * Timestamp when promotion was executed
     */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    /**
     * Notes about the batch
     */
    private String notes;

    /**
     * Director's approval/comment
     */
    @Column(name = "director_comment")
    private String directorComment;
}

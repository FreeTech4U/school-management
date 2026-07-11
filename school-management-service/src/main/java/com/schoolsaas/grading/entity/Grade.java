package com.schoolsaas.grading.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "grades")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grade extends BaseEntity {

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "class_subject_id", nullable = false)
    private UUID classSubjectId;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(name = "evaluation_type", nullable = false)
    private String evaluationType; // DEVOIR, COMPOSITION, ORAL, TP

    @Column(name = "evaluation_label", nullable = false)
    private String evaluationLabel;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "entered_by")
    private UUID enteredBy;

    private String comment;
}

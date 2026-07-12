package com.schoolsaas.grading.entity;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.EvaluationType;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_subject_id", nullable = false)
    private ClassSubject classSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(name = "evaluation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EvaluationType evaluationType;

    @Column(name = "evaluation_label", nullable = false)
    private String evaluationLabel;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "entered_by")
    private UUID enteredBy;

    private String comment;
}

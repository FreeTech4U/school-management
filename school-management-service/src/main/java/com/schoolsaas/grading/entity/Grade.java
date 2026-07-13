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

/**
 * Note obtenue par un élève dans une matière, sur un trimestre (F-15).
 *
 * CORRECTION — les trois @ManyToOne ont été remplacées par des UUID :
 *     StudentEnrollment  (grading → enrollment)
 *     ClassSubject       (grading → academic)
 *     Term               (grading → academic)
 *   Elles violaient la règle inter-domaines et créaient les cycles
 *   grading ↔ enrollment et grading ↔ academic.
 *
 * Pour calculer une moyenne pondérée, GradeService a besoin du coefficient porté
 * par ClassSubject : il l'obtient via AcademicService.getClassSubjects(classId),
 * en un seul appel, et fait la jointure en mémoire. C'est le prix — modeste — de
 * l'indépendance des domaines.
 *
 * RÈGLES :
 *   • Saisie autorisée uniquement si Term.gradesEntryOpen = true
 *     → sinon erreur GRADES_ENTRY_CLOSED.
 *   • Un TEACHER ne peut saisir que sur les ClassSubject dont il est titulaire.
 *   • Pas deux évaluations de même libellé pour un même élève / matière /
 *     trimestre → erreur DUPLICATE_GRADE (contrainte UNIQUE en base).
 */
@Entity
@Table(
        name = "grades",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_grade",
                columnNames = {"enrollment_id", "class_subject_id", "term_id", "evaluation_label"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grade extends BaseEntity {

    /** INTER-domaine (grading → enrollment) : UUID. */
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    /** INTER-domaine (grading → academic) : UUID. Porte le coefficient. */
    @Column(name = "class_subject_id", nullable = false)
    private UUID classSubjectId;

    /** INTER-domaine (grading → academic) : UUID. */
    @Column(name = "term_id", nullable = false)
    private UUID termId;

    /** Note sur 20. CHECK (value BETWEEN 0 AND 20) en base. */
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal value;

    /** DEVOIR · COMPOSITION · ORAL · TP */
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, length = 20)
    private EvaluationType evaluationType;

    /** Ex : "Devoir 1", "Composition T1". Entre dans la contrainte d'unicité. */
    @Column(name = "evaluation_label", nullable = false, length = 100)
    private String evaluationLabel;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    /** Enseignant ayant saisi la note. INTER-domaine (→ identity) : UUID. */
    @Column(name = "entered_by")
    private UUID enteredBy;

    @Column(columnDefinition = "TEXT")
    private String comment;
}


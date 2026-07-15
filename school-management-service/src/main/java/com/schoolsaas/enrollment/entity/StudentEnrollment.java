package com.schoolsaas.enrollment.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.EnrollmentStatus;
import com.schoolsaas.common.enums.PromotionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inscription d'un élève dans une classe, pour une année scolaire.
 *
 * C'est la table PIVOT du modèle : notes, présences, frais et bulletins s'y
 * rattachent. Un élève a une inscription par année (contrainte UNIQUE).
 *
 * CORRECTION MAJEURE — les 4 collections ont été SUPPRIMÉES :
 *     List<StudentFee>  (enrollment → finance)
 *     List<Grade>       (enrollment → grading)
 *     List<Attendance>  (enrollment → attendance)
 *     List<ReportCard>  (enrollment → grading)
 *
 *   Elles violaient la règle d'architecture — le domaine enrollment/ importait
 *   les entités de trois autres domaines — et créaient des cycles de dépendance
 *   (enrollment ↔ finance, enrollment ↔ grading, enrollment ↔ attendance).
 *
 *   Surtout, le « cascade = ALL, orphanRemoval = true » sur fees signifiait que
 *   supprimer une inscription effaçait en cascade les frais, donc les
 *   payment_allocations, donc l'historique comptable des paiements reçus.
 *   Une bombe à retardement en production.
 *
 *   Pour obtenir les frais d'une inscription : FinanceService.getFeesByEnrollment(id).
 *   Pour ses notes : GradeService.getGradesByEnrollment(id).
 */
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_enrollment_per_year",
                columnNames = {"student_id", "academic_year_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollment extends BaseEntity {

    /** INTRA-domaine (enrollment → enrollment) : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** INTER-domaine (enrollment → academic) : UUID. */
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    /** INTER-domaine (enrollment → academic) : UUID. */
    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "enrollment_date", nullable = false)
    @Builder.Default
    private LocalDate enrollmentDate = LocalDate.now();

    @Column(name = "is_repeating", nullable = false)
    @Builder.Default
    private Boolean isRepeating = false;

    /**
     * ENROLLED · TRANSFERRED · WITHDRAWN · GRADUATED
     * Seul ENROLLED compte dans les effectifs et la génération des frais.
     * La vue matérialisée mv_dashboard_stats filtre sur cette valeur.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    /** PENDING · PROMOTED · REPEATED · GRADUATED (F-19). */
    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_status", nullable = false, length = 20)
    @Builder.Default
    private PromotionStatus promotionStatus = PromotionStatus.PENDING;

    /**
     * AJOUT — traçabilité de la décision de passage (F-19).
     * Ces deux colonnes existaient en base mais pas dans l'entité. Elles rendent
     * inutile un statut « OVERRIDDEN » : un override du directeur produit un
     * statut FINAL (PROMOTED ou REPEATED) et laisse sa signature ici.
     * INTER-domaine (→ identity) : UUID.
     */
    @Column(name = "promotion_validated_by")
    private UUID promotionValidatedBy;

    @Column(name = "promotion_validated_at")
    private Instant promotionValidatedAt;

    /** Obligatoire si status = TRANSFERRED (vérifié par le service). */
    @Column(name = "transfer_notes", length = 500)
    private String transferNotes;

    /** Moyenne annuelle. Base de la décision de passage. */
    @Column(name = "final_average", precision = 5, scale = 2)
    private BigDecimal finalAverage;

    /** AJOUT — colonne présente en base, absente de l'entité précédente. */
    @Column(columnDefinition = "TEXT")
    private String notes;
}
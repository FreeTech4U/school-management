package com.schoolsaas.enrollment.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.PromotionBatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Traitement de promotion de fin d'année, classe par classe (F-19).
 *
 * Ce n'est PAS une donnée métier : c'est un journal d'orchestration. Il permet
 * de dérouler une promotion en trois temps, au lieu d'un bouton irréversible qui
 * écrirait directement dans enrollments :
 *
 *   CREATED   → le système calcule les propositions
 *               (moyenne ≥ seuil → PROMOTED, sinon REPEATED).
 *               Rien n'est encore écrit dans enrollments.
 *   VALIDATED → le directeur a relu, ajusté les cas limites, et confirmé.
 *   EXECUTED  → les inscriptions de l'année suivante sont réellement créées.
 *   CANCELLED → traitement abandonné.
 *
 * CORRECTION : status passe d'un String libre à l'enum PromotionBatchStatus.
 *
 * NOTE : prévu pour la phase 3. L'entité existe, le service correspondant n'est
 * pas implémenté dans le MVP.
 */
@Entity
@Table(name = "promotion_batches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionBatch extends BaseEntity {

    /** Année d'origine. INTER-domaine (→ academic) : UUID. */
    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    /** Année de destination. */
    @Column(name = "next_academic_year_id", nullable = false)
    private UUID nextAcademicYearId;

    /** Classe traitée. */
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PromotionBatchStatus status = PromotionBatchStatus.CREATED;

    @Column(name = "promoted_count", nullable = false)
    @Builder.Default
    private Integer promotedCount = 0;

    @Column(name = "repeated_count", nullable = false)
    @Builder.Default
    private Integer repeatedCount = 0;

    @Column(name = "graduated_count", nullable = false)
    @Builder.Default
    private Integer graduatedCount = 0;

    @Column(name = "total_processed", nullable = false)
    @Builder.Default
    private Integer totalProcessed = 0;

    /**
     * Blocages empêchant l'exécution, ex :
     * « 3 élèves sans moyenne finale — bulletins du T3 non publiés ».
     */
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    /** CORRECTION : Instant (colonne TIMESTAMPTZ). */
    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "director_comment", columnDefinition = "TEXT")
    private String directorComment;
}


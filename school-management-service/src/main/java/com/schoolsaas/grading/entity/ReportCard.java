package com.schoolsaas.grading.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.ReportCardStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Bulletin scolaire d'un élève pour un trimestre (F-16).
 *
 * CYCLE DE VIE :
 *   DRAFT ──generateForClass()──▶ DRAFT   (moyennes et rang calculés)
 *         ──publish()──────────▶ PUBLISHED      (PDF généré, publishedAt posé)
 *         ──SMS au parent──────▶ SENT_TO_PARENT (état terminal)
 *
 * CALCUL DE LA MOYENNE :
 *   generalAverage = Σ(moyenne_matière × coefficient) / Σ(coefficients)
 *
 * CORRECTION — StudentEnrollment et Term passent d'@ManyToOne à des UUID
 * (violation de la règle inter-domaines : grading → enrollment et academic).
 *
 * CORRECTION — publishedAt passe de LocalDateTime à Instant (colonne TIMESTAMPTZ).
 *
 * Un CHECK en base garantit qu'un bulletin publié possède forcément une moyenne
 * ET une date de publication.
 */
@Entity
@Table(
        name = "report_cards",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_report_card",
                columnNames = {"enrollment_id", "term_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCard extends BaseEntity {

    /** INTER-domaine (grading → enrollment) : UUID. */
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    /** INTER-domaine (grading → academic) : UUID. */
    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "general_average", precision = 5, scale = 2)
    private BigDecimal generalAverage;

    @Column(name = "rank_in_class")
    private Short rankInClass;

    @Column(name = "class_size")
    private Short classSize;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "director_comment", columnDefinition = "TEXT")
    private String directorComment;

    /** DRAFT · PUBLISHED · SENT_TO_PARENT */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportCardStatus status = ReportCardStatus.DRAFT;

    /** Renseigné à la publication (génération PDF asynchrone). */
    @Column(name = "pdf_url")
    private String pdfUrl;

    /** CORRECTION : Instant (colonne TIMESTAMPTZ). */
    @Column(name = "published_at")
    private Instant publishedAt;
}

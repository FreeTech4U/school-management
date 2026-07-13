package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.entity.ReportCard;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Trimestre d'une année scolaire.
 *
 * CORRECTION — les collections List<Grade> et List<ReportCard> ont été
 * SUPPRIMÉES. Elles violaient la règle d'architecture : le domaine academic/
 * importait les entités du domaine grading/, créant un cycle
 * academic ↔ grading.
 *
 * Pire, le cascade = ALL + orphanRemoval signifiait que supprimer un trimestre
 * effaçait silencieusement toutes les notes et tous les bulletins associés.
 *
 * Pour lire les notes d'un trimestre : passer par GradeService, qui interroge
 * son propre repository avec le termId.
 */
@Entity
@Table(
        name = "terms",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_term_number_per_year",
                columnNames = {"academic_year_id", "term_number"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Term extends BaseEntity {

    /** Association INTRA-domaine (academic → academic) : @ManyToOne autorisé. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /** Ex : "1er Trimestre". */
    @Column(nullable = false, length = 100)
    private String name;

    /** 1 à 4. */
    @Column(name = "term_number", nullable = false)
    private Short termNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Un seul trimestre courant PAR ANNÉE (index UNIQUE partiel en base). */
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isCurrent = false;

    /**
     * Ouvre ou ferme la saisie des notes (F-05).
     * Seul le DIRECTOR peut le modifier. Un enseignant qui tente de saisir une
     * note alors que ce drapeau est à false reçoit l'erreur GRADES_ENTRY_CLOSED.
     */
    @Column(name = "grades_entry_open", nullable = false)
    @Builder.Default
    private Boolean gradesEntryOpen = false;
}


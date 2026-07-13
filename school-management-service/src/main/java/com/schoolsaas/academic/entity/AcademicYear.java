package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.YearStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Année scolaire (ex : « 2024-2025 »).
 *
 * RÈGLE MÉTIER (F-04) : une seule année peut être courante à la fois.
 * L'unicité est garantie en base par un index UNIQUE partiel
 * (uq_academic_year_current). Côté service, positionner isCurrent = true sur une
 * année impose de remettre toutes les autres à false AVANT, dans la même
 * transaction — sinon l'index rejette l'opération.
 */
@Entity
@Table(name = "academic_years")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear extends BaseEntity {

    /** Libellé unique, ex : "2024-2025". */
    @Column(nullable = false, unique = true, length = 50)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Année en cours. Une seule à TRUE dans tout le schema.
     * @Builder.Default : sans cette annotation, Lombok IGNORE l'initialiseur et
     * le builder produirait null → violation de la contrainte NOT NULL.
     */
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isCurrent = false;

    /** ACTIVE : en cours · CLOSED : clôturée, plus modifiable. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private YearStatus status = YearStatus.ACTIVE;
}


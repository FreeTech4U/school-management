package com.schoolsaas.finance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.FeeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Grille tarifaire de l'école pour une année scolaire (F-10).
 *
 * classId NULL signifie « frais applicable à TOUTES les classes de l'année »
 * (frais général, ex : inscription identique pour tous).
 *
 * UNICITÉ : une seule structure par (année, classe, type). En base, cela demande
 * DEUX index UNIQUE partiels, car une contrainte UNIQUE classique est inopérante
 * quand class_id vaut NULL (en SQL, NULL <> NULL).
 */
@Entity
@Table(name = "fee_structures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructure extends BaseEntity {

    /** INTER-domaine (finance → academic) : UUID. */
    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    /** INTER-domaine (finance → academic) : UUID. NULL = toutes les classes. */
    @Column(name = "class_id")
    private UUID classId;

    /** TUITION · REGISTRATION · CANTEEN · TRANSPORT · EXAM · ACTIVITY · OTHER */
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType;

    @Column(nullable = false, length = 255)
    private String label;

    /** Montant en GNF. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * CORRECTION : NULLABLE (le code précédent imposait nullable = false).
     * Certains frais n'ont pas d'échéance fixe. La colonne SQL est nullable.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "installments_allowed", nullable = false)
    @Builder.Default
    private Boolean installmentsAllowed = false;

    @Column(name = "max_installments")
    @Builder.Default
    private Short maxInstallments = 3;
}
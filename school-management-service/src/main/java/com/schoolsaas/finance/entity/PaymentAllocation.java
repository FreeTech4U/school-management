package com.schoolsaas.finance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Imputation d'un paiement sur un frais précis (F-12).
 *
 * Un versement unique de 500 000 GNF peut couvrir simultanément la scolarité,
 * la cantine et le transport : une ligne d'allocation par frais.
 *
 * RÈGLE : la somme des allocations doit être égale au montant du paiement.
 * Vérifiée par PaymentService (code d'erreur ALLOCATION_MISMATCH).
 *
 * CORRECTION : la validation @PrePersist qui levait des IllegalArgumentException
 * a été supprimée. Les contraintes CHECK en base et la Bean Validation sur les
 * DTO d'entrée couvrent déjà ces cas — et produisent des erreurs 400 explicites
 * plutôt que des 500 opaques.
 *
 * Toute écriture ici déclenche le trigger fn_recalculate_fee_status, qui
 * recalcule amount_paid et status sur le StudentFee concerné.
 */
@Entity
@Table(
        name = "payment_allocations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_payment_allocation",
                columnNames = {"payment_id", "student_fee_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocation extends BaseEntity {

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_fee_id", nullable = false)
    private StudentFee studentFee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
}
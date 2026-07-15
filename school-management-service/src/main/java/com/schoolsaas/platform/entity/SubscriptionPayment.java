package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.SubscriptionPaymentMethod;
import com.schoolsaas.common.enums.SubscriptionPaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Paiement d'un abonnement : l'ÉCOLE paie SCHOOLSAAS.
 *
 * ⚠ Schema PUBLIC.
 *
 * ⚠ NE PAS CONFONDRE avec finance/Payment (schema tenant), où le PARENT paie
 *   l'ÉCOLE. Deux flux d'argent distincts, deux jeux d'enums :
 *     • SubscriptionPaymentMethod : pas de CASH (SchoolSaaS est opéré à
 *       distance, il n'y a pas de guichet), BANK_TRANSFER dominant.
 *     • PaymentMethod : CASH dominant (frais de scolarité au guichet).
 *
 * CORRECTIONS :
 *   • paymentDate passe de LocalDateTime à LocalDate (colonne DATE).
 *   • AJOUT de periodStart / periodEnd : sans elles, impossible de savoir si un
 *     versement couvre un mois ou une année, ni de produire un échéancier.
 *   • AJOUT du champ notes.
 */
@Entity
@Table(name = "subscription_payments", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPayment extends BaseEntity {

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SchoolSubscription subscription;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GNF";

    /** CORRECTION : LocalDate (colonne DATE), et non LocalDateTime. */
    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private LocalDate paymentDate = LocalDate.now();

    /** BANK_TRANSFER · ORANGE_MONEY · MTN_MONEY · WAVE · CARD · CHECK */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private SubscriptionPaymentMethod paymentMethod;

    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionReference;

    /** PENDING · COMPLETED (défaut) · FAILED · REFUNDED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionPaymentStatus status = SubscriptionPaymentStatus.COMPLETED;

    /** AJOUT — période couverte par ce versement. */
    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    /** AJOUT — colonne présente en base, absente de l'entité précédente. */
    @Column(columnDefinition = "TEXT")
    private String notes;
}

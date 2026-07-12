package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Enregistre les paiements effectués pour les abonnements.
 */
@Entity
@Table(name = "subscription_payments", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPayment extends BaseEntity {

    /** Abonnement associé à ce paiement */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SchoolSubscription subscription;

    /** Montant payé */
    @Column(nullable = false)
    private BigDecimal amount;

    /** Devise du paiement */
    @Column(length = 3)
    private String currency = "GNF";

    /** Date et heure du paiement */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate = LocalDateTime.now();

    /** Méthode de paiement (Orange Money, Virement, etc.) */
    @Column(name = "payment_method")
    private String paymentMethod;

    /** Référence unique de la transaction (fournie par la passerelle de paiement) */
    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    /** Statut du paiement (completed, pending, failed) */
    @Column(nullable = false)
    private String status = "completed";
}

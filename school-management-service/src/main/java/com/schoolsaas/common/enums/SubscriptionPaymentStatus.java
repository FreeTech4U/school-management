package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'un paiement d'abonnement SaaS (l'école paie SchoolSaaS).
 *
 * À ne pas confondre avec PaymentStatus, qui concerne les paiements de frais
 * de scolarité (le parent paie l'école) et vit dans le schema tenant.
 *
 * PENDING est conservé ici — contrairement à PaymentStatus — car un virement
 * bancaire d'abonnement peut être annoncé avant d'être crédité.
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionPaymentStatus {

    /** Paiement annoncé, en attente de confirmation bancaire. */
    PENDING   ("En attente"),

    /** Paiement encaissé. */
    COMPLETED ("Encaissé"),

    /** Paiement échoué (virement rejeté, transaction mobile money annulée). */
    FAILED    ("Échec"),

    /** Remboursé à l'école. */
    REFUNDED  ("Remboursé");

    private final String label;
}

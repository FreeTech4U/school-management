package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Moyens de paiement acceptés pour l'abonnement SaaS (l'école paie SchoolSaaS).
 *
 * Volontairement DISTINCT de PaymentMethod (frais de scolarité, schema tenant) :
 *   • BANK_TRANSFER est ici le mode dominant (facturation annuelle B2B),
 *     alors que CASH domine côté frais de scolarité.
 *   • CASH n'a pas de sens ici : SchoolSaaS est opéré à distance depuis la
 *     France, il n'y a pas de guichet.
 *   • Les deux concepts peuvent diverger avec le temps (ajout de Stripe pour
 *     l'abonnement, sans impact sur les paiements de scolarité).
 *
 * CORRECTION : 'card' figurait dans le script public alors que CREDIT_CARD
 * avait été retiré du tenant. Conservé ici sous le nom CARD, car un paiement
 * par carte est plausible pour un abonnement SaaS (via une passerelle en ligne).
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionPaymentMethod {

    /** Virement bancaire — mode dominant pour la facturation annuelle. */
    BANK_TRANSFER ("Virement bancaire"),

    /** Orange Money. */
    ORANGE_MONEY  ("Orange Money"),

    /** MTN Mobile Money. */
    MTN_MONEY     ("MTN Money"),

    /** Wave. */
    WAVE          ("Wave"),

    /** Carte bancaire (passerelle de paiement en ligne). */
    CARD          ("Carte bancaire"),

    /** Chèque. */
    CHECK         ("Chèque");

    private final String label;
}

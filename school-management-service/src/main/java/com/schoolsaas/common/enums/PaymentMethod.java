package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Moyens de paiement acceptés par les écoles (Guinée / Afrique de l'Ouest).
 *
 * CORRECTION MAJEURE vs version précédente :
 *   Les moyens de paiement réels du marché guinéen avaient disparu au profit
 *   d'un MOBILE_MONEY générique.
 *
 *   - MOBILE_MONEY  → éclaté en ORANGE_MONEY / MTN_MONEY / WAVE
 *                     Un directeur doit pouvoir répondre à "combien a-t-on
 *                     encaissé via Orange Money ce mois-ci ?". Avec un
 *                     MOBILE_MONEY générique, cette question est sans réponse.
 *   - CREDIT_CARD   → supprimé (quasi inexistant en Guinée pour la scolarité)
 *   - WIRE_TRANSFER → supprimé (doublon de BANK_TRANSFER)
 *   - CRYPTO        → supprimé (aucun usage réel dans le contexte cible)
 *
 * Le champ Payment.referenceNumber porte la référence de la transaction
 * (ex: numéro de transaction Orange Money, référence du virement).
 *
 * Type PostgreSQL correspondant : payment_method
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

    /** Espèces au guichet. Méthode dominante en Guinée. */
    CASH          ("Espèces",           false),

    /** Orange Money — opérateur mobile money n°1 en Guinée. */
    ORANGE_MONEY  ("Orange Money",      true),

    /** MTN Mobile Money. */
    MTN_MONEY     ("MTN Money",         true),

    /** Wave — mobile money à frais réduits, en croissance en Afrique de l'Ouest. */
    WAVE          ("Wave",              true),

    /** Virement bancaire. */
    BANK_TRANSFER ("Virement bancaire", true),

    /** Chèque bancaire. */
    CHECK         ("Chèque",            true);

    private final String  label;

    /**
     * True si une référence de transaction doit être saisie.
     * Utilisé par le formulaire Flutter pour afficher/masquer le champ
     * "Référence" et par la validation côté backend.
     */
    private final boolean requiresReference;
}


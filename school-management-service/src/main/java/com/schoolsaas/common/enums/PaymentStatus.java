package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'un paiement.
 *
 * BUG CORRIGÉ : CANCELLED existait dans l'enum Java mais PAS dans le type
 *               PostgreSQL payment_status. Le premier appel à cancelPayment()
 *               aurait planté :
 *                 ERROR: invalid input value for enum payment_status: "CANCELLED"
 *
 * SIMPLIFICATION :
 *   - FAILED             → supprimé. Un paiement en espèces ou Orange Money est
 *                          encaissé AVANT d'être saisi dans l'application. Il n'y
 *                          a pas de passerelle de paiement en ligne : un paiement
 *                          ne peut donc pas "échouer" côté système.
 *   - PARTIALLY_REFUNDED → supprimé. Un remboursement partiel se modélise par un
 *                          nouveau paiement négatif (avoir), pas par un statut.
 *
 * MODÈLE D'ANNULATION (remplace le "soft delete via notes" des specs) :
 *   Un paiement n'est JAMAIS supprimé. cancelPayment() passe le statut à
 *   CANCELLED et renseigne cancellation_reason / cancelled_by / cancelled_at.
 *   → traçabilité comptable complète, et le trigger fn_recalculate_fee_status
 *     ignore les allocations des paiements CANCELLED.
 *
 * Type PostgreSQL correspondant : payment_status
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    /** Paiement enregistré et encaissé. Statut par défaut. */
    CONFIRMED ("Confirmé"),

    /** Paiement annulé (erreur de saisie, dans les 24h). Allocations neutralisées. */
    CANCELLED ("Annulé"),

    /** Paiement remboursé au parent (départ de l'élève, trop-perçu). */
    REFUNDED  ("Remboursé");

    private final String label;

    /** Seul un paiement confirmé compte dans le calcul du solde d'un frais. */
    public boolean countsTowardsBalance() {
        return this == CONFIRMED;
    }
}


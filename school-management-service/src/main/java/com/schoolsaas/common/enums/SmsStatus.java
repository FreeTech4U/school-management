package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'un SMS dans le journal d'envoi (F-13).
 *
 * FLUX :
 *   PENDING --SmsProvider.send()--> SENT      (accepté par l'opérateur)
 *                                --> FAILED    (rejeté : numéro invalide, crédit épuisé)
 *   SENT    --accusé de réception--> DELIVERED (reçu sur le téléphone)
 *
 * CORRECTION vs version précédente :
 *   - QUEUED    → supprimé (doublon exact de PENDING)
 *   - BOUNCED   → supprimé (cas couvert par FAILED + error_code)
 *   - OPTED_OUT → supprimé (le désabonnement d'un parent est une propriété du
 *                 destinataire, pas d'un message. Il relève d'un futur champ
 *                 Student.smsOptOut, pas du statut d'un SMS individuel.)
 *
 * Type PostgreSQL correspondant : sms_status
 */
@Getter
@RequiredArgsConstructor
public enum SmsStatus {

    /** SMS créé en base, pas encore transmis à l'opérateur. Statut initial. */
    PENDING   ("En attente"),

    /** Transmis à l'opérateur (Orange, Twilio) et accepté par lui. */
    SENT      ("Envoyé"),

    /** Accusé de réception confirmé : le SMS est arrivé sur le téléphone. */
    DELIVERED ("Distribué"),

    /** Échec définitif. Les champs error_code / error_message sont renseignés. */
    FAILED    ("Échec");

    private final String label;

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED;
    }
}


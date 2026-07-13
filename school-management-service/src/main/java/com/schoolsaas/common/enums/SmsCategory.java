package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catégorie d'un template SMS et d'un SMS envoyé (F-13).
 *
 * AJOUT : cet enum n'existait pas. Les colonnes sms_templates.category et
 *         sms_logs.category étaient des VARCHAR libres (avec un CHECK sur
 *         l'une des deux seulement, et aucun sur l'autre).
 *
 * Permet au directeur de filtrer son journal SMS ("combien de relances de
 * paiement ce mois-ci ?") et de désactiver une catégorie entière si besoin.
 *
 * Type PostgreSQL correspondant : sms_category
 */
@Getter
@RequiredArgsConstructor
public enum SmsCategory {

    /** Relances de paiement, confirmations de paiement. */
    FINANCIAL      ("Financier"),

    /** Bulletins publiés, absences. */
    ACADEMIC       ("Scolaire"),

    /** Convocations, informations générales de l'école. */
    ADMINISTRATIVE ("Administratif"),

    /** Message libre rédigé par le directeur. */
    CUSTOM         ("Personnalisé");

    private final String label;
}

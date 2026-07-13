package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Période de la journée concernée par un appel de présence (F-17).
 *
 * Conforme aux specs. EVENING est conservé : certaines écoles privées
 * guinéennes proposent des cours du soir.
 *
 * Contrainte d'unicité en base : un seul enregistrement par
 * (enrollment_id, date, period).
 *
 * Type PostgreSQL correspondant : period
 */
@Getter
@RequiredArgsConstructor
public enum Period {

    /** Journée entière (un seul appel par jour). Valeur par défaut. */
    FULL_DAY  ("Journée entière"),

    /** Matinée uniquement. */
    MORNING   ("Matin"),

    /** Après-midi uniquement. */
    AFTERNOON ("Après-midi"),

    /** Cours du soir. */
    EVENING   ("Soir");

    private final String label;
}

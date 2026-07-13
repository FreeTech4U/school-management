package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Nature d'un frais de scolarité (F-10).
 *
 * Conforme aux specs. ACTIVITY et OTHER sont des extensions volontaires :
 * les écoles guinéennes facturent souvent des sorties, des fournitures ou
 * des tenues — sans ces valeurs, elles seraient forcées de détourner un
 * autre type.
 *
 * Contrainte d'unicité en base : une seule FeeStructure par
 * (academic_year_id, class_id, fee_type).
 *
 * Type PostgreSQL correspondant : fee_type
 */
@Getter
@RequiredArgsConstructor
public enum FeeType {

    /** Frais de scolarité (le frais principal, souvent échelonné). */
    TUITION      ("Frais de scolarité"),

    /** Frais d'inscription, dus une fois en début d'année. */
    REGISTRATION ("Frais d'inscription"),

    /** Cantine scolaire. */
    CANTEEN      ("Cantine"),

    /** Transport scolaire. */
    TRANSPORT    ("Transport"),

    /** Frais d'examen (BEPC, BAC, examens blancs). */
    EXAM         ("Frais d'examen"),

    /** Sorties scolaires, activités parascolaires. */
    ACTIVITY     ("Activités"),

    /** Fournitures, tenue scolaire, autres frais divers. */
    OTHER        ("Autres frais");

    private final String label;
}

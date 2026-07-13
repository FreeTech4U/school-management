package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Jour de la semaine d'un créneau horaire (F-18).
 *
 * ATTENTION : ne PAS utiliser java.time.DayOfWeek à la place de cet enum.
 *
 * Deux raisons :
 *   1. java.time.DayOfWeek inclut SUNDAY. Les écoles guinéennes n'ont pas cours
 *      le dimanche : autoriser cette valeur permettrait la saisie d'un créneau
 *      aberrant, qu'aucune contrainte ne rejetterait côté Java.
 *   2. Un enum du JDK ne peut pas porter de champ label français, ni évoluer
 *      selon les besoins métier.
 *
 * La contrainte CHECK de la table time_slots est alignée sur ces 6 valeurs.
 */
@Getter
@RequiredArgsConstructor
public enum DayOfWeek {

    MONDAY    ("Lundi",    1),
    TUESDAY   ("Mardi",    2),
    WEDNESDAY ("Mercredi", 3),
    THURSDAY  ("Jeudi",    4),
    FRIDAY    ("Vendredi", 5),
    SATURDAY  ("Samedi",   6);

    private final String label;

    /** Ordre d'affichage dans la grille hebdomadaire (lundi = 1). */
    private final int    order;
}

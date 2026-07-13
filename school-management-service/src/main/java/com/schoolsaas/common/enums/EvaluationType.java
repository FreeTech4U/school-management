package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Type d'évaluation d'une note (F-15).
 *
 * Conforme aux specs — aucune correction nécessaire.
 *
 * Terminologie du système scolaire francophone (Guinée, France, Sénégal...).
 * Un élève a plusieurs notes par matière et par trimestre ; la moyenne de la
 * matière est calculée à partir de l'ensemble de ces évaluations, pondérée par
 * le coefficient du ClassSubject.
 *
 * Contrainte d'unicité en base :
 *   (enrollment_id, class_subject_id, term_id, evaluation_label)
 *   → deux "Devoir 1" en Maths au T1 pour le même élève sont impossibles.
 *
 * Type PostgreSQL correspondant : evaluation_type
 */
@Getter
@RequiredArgsConstructor
public enum EvaluationType {

    /** Devoir écrit en classe ou à la maison. */
    DEVOIR      ("Devoir"),

    /** Composition trimestrielle (évaluation principale du trimestre). */
    COMPOSITION ("Composition"),

    /** Interrogation ou exposé oral. */
    ORAL        ("Oral"),

    /** Travaux pratiques (sciences, informatique). */
    TP          ("Travaux pratiques");

    private final String label;
}

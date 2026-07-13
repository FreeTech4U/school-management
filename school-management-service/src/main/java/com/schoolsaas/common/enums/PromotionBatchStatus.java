package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'un traitement de promotion par lot (F-19).
 *
 * AJOUT : ce champ était un String libre dans PromotionBatch, alors que toutes
 * les autres énumérations du projet sont des enums. Un String n'offre ni
 * auto-complétion, ni sécurité au refactoring, ni garantie de cohérence avec la
 * contrainte CHECK de la base.
 *
 * FLUX :
 *   CREATED   → propositions calculées, rien n'est encore écrit dans enrollments
 *   VALIDATED → le directeur a relu et confirmé
 *   EXECUTED  → les inscriptions de l'année suivante sont réellement créées
 *   CANCELLED → traitement abandonné
 */
@Getter
@RequiredArgsConstructor
public enum PromotionBatchStatus {

    CREATED   ("Créé"),
    VALIDATED ("Validé"),
    EXECUTED  ("Exécuté"),
    CANCELLED ("Annulé");

    private final String label;

    /** Un lot exécuté ou annulé ne peut plus être modifié. */
    public boolean isFinal() {
        return this == EXECUTED || this == CANCELLED;
    }
}

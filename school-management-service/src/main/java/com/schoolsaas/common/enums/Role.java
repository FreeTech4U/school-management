package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Rôles des utilisateurs de l'application.
 *
 * Matrice des droits (voir SPECS TX-02) :
 *   DIRECTOR   : accès total (seul à pouvoir supprimer, publier les bulletins,
 *                ouvrir/fermer la saisie des notes, valider les promotions)
 *   TEACHER    : notes + présences, uniquement sur ses propres ClassSubject
 *   ACCOUNTANT : élèves (lecture/écriture), frais, paiements
 *   PARENT     : consultation seule (application parent — phase 3)
 *
 * Type PostgreSQL correspondant : role
 */
@Getter
@RequiredArgsConstructor
public enum Role {

    DIRECTOR   ("Directeur"),
    TEACHER    ("Enseignant"),
    ACCOUNTANT ("Comptable"),
    PARENT     ("Parent");

    private final String label;
}

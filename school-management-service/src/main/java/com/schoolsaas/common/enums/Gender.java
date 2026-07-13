package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sexe de l'élève.
 *
 * AJOUT : le champ était un String de longueur 1 dans l'entité Student, contrôlé
 * uniquement par une contrainte CHECK en base. Rien n'empêchait le code Java
 * d'écrire "X" ou "m" (minuscule) — l'erreur n'apparaissait qu'au moment du
 * flush, sans indication utile.
 *
 * La colonne SQL reste CHAR(1) : le mapping se fait via un AttributeConverter
 * (voir GenderConverter) plutôt qu'avec @Enumerated, car EnumType.STRING
 * écrirait "MALE" / "FEMALE" dans une colonne d'un seul caractère.
 */
@Getter
@RequiredArgsConstructor
public enum Gender {

    MALE   ("M", "Masculin"),
    FEMALE ("F", "Féminin");

    /** Valeur stockée en base (colonne CHAR(1), contrainte CHECK IN ('M','F')). */
    private final String code;

    private final String label;

    public static Gender fromCode(String code) {
        if (code == null) return null;
        for (Gender g : values()) {
            if (g.code.equals(code)) return g;
        }
        throw new IllegalArgumentException("Genre inconnu : " + code);
    }
}

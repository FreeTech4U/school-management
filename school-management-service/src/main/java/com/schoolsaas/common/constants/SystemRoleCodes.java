package com.schoolsaas.common.constants;

/**
 * Codes des rôles connus du cœur applicatif (bootstrapping).
 *
 * NE remplace PAS la table public.roles — ce n'est PAS un enum fermé, juste
 * un filet de sécurité pour éviter les fautes de frappe partout où le code
 * a besoin de désigner un rôle BIEN CONNU du système (ex : "assigner
 * DIRECTOR au créateur d'une école à l'onboarding", F-01).
 *
 * Un rôle ajouté plus tard via l'API d'administration (ex : "INFIRMIER")
 * n'a PAS besoin d'une constante ici : le reste du système manipule les
 * rôles comme des codes String (Role.getCode()), génériquement, sans
 * connaître à la compilation la liste complète des rôles possibles.
 */
public final class SystemRoleCodes {

    public static final String DIRECTOR   = "DIRECTOR";
    public static final String TEACHER    = "TEACHER";
    public static final String ACCOUNTANT = "ACCOUNTANT";
    public static final String PARENT     = "PARENT";

    private SystemRoleCodes() {}
}
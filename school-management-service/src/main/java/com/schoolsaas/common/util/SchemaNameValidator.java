package com.schoolsaas.common.util;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/**
 * Garde-fou anti-injection SQL pour les noms de schéma PostgreSQL.
 *
 * Un nom de schéma est concaténé directement dans des requêtes (SET
 * search_path, CREATE SCHEMA, SELECT ... FROM <schema>.table) car PostgreSQL
 * n'autorise pas les paramètres préparés (?) pour les identifiants d'objets.
 * Cette validation est l'unique rempart contre l'injection à ces endroits.
 *
 * Reflète la contrainte chk_school_schema_name de V1__init_public_schema.sql :
 * minuscules/chiffres/underscore, et VARCHAR(63) — la limite stricte de
 * PostgreSQL pour un identifiant, au-delà de laquelle il serait tronqué
 * silencieusement.
 */
@UtilityClass
public class SchemaNameValidator {

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private static final int MAX_LENGTH = 63;

    public static boolean isValid(String schemaName) {
        return schemaName != null
                && schemaName.length() <= MAX_LENGTH
                && SCHEMA_NAME_PATTERN.matcher(schemaName).matches();
    }
}

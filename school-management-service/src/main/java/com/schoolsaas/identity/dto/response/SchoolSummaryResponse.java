package com.schoolsaas.identity.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Résumé d'une école pour le sélecteur multi-écoles (dropdown "mes écoles").
 * Utilisé par GET /auth/my-schools, une fois déjà authentifié.
 */
@Getter
@AllArgsConstructor
public class SchoolSummaryResponse {

    private final String slug;
    private final String name;
    private final String rolesSnapshot;
}
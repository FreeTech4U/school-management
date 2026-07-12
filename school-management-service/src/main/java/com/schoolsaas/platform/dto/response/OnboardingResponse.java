package com.schoolsaas.platform.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Réponse renvoyée après un onboarding réussi.
 */
@Data
@Builder
public class OnboardingResponse {
    /** Slug du tenant créé */
    private String tenantSlug;

    /** Nom du schéma PostgreSQL généré */
    private String schemaName;

    /** Nom de l'école */
    private String schoolName;

    /** Message de confirmation */
    private String message;
}

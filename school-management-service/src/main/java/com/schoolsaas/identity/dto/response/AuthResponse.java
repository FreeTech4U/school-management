package com.schoolsaas.identity.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private long   expiresIn;

    /**
     * Signale au frontend que l'école habituelle (lastConnectedSchool) était
     * indisponible (suspendue) et qu'un repli automatique a eu lieu vers une
     * autre école active. Permet d'afficher un message explicite ("vous êtes
     * connecté à X au lieu de Y") plutôt qu'une redirection silencieuse.
     */
    @Builder.Default
    private boolean redirectedToFallbackSchool = false;

    private UserData user;

    @Getter
    @Builder
    public static class UserData {
        private UUID         id;
        private String       fullName;
        private String       email;
        private List<String> roles;

        /** UUID de l'école (usage frontend). À NE PAS confondre avec le
         *  claim JWT "tenantId", qui porte le NOM DU SCHEMA (String). */
        private UUID          tenantId;

        private String        schoolName;

        /** Utile au frontend pour construire ses URLs/routes. */
        private String        schoolSlug;
    }
}
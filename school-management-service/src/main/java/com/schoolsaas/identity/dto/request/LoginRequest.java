package com.schoolsaas.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Requête de connexion (F-02a).
 *
 * tenantSlug n'est PLUS obligatoire : le flux normal ne demande que l'email
 * et le mot de passe. AuthService résout automatiquement l'école via
 * Person.lastConnectedSchool (avec repli aléatoire si celle-ci est devenue
 * indisponible).
 *
 * tenantSlug reste disponible en option avancée, pour le cas rare où une
 * personne aurait des mots de passe différents selon les écoles.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    /** Optionnel. Si fourni, cible explicitement cette école par son slug. */
    private String tenantSlug;
}

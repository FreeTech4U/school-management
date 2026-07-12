package com.schoolsaas.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requête d'inscription pour une nouvelle école.
 * Regroupe les informations de l'école, du plan choisi et du compte directeur.
 */
@Data
public class OnboardingRequest {

    /** Nom complet de l'établissement */
    @NotBlank(message = "Le nom de l'école est obligatoire")
    private String schoolName;

    /** Slug unique pour l'URL et le schéma (ex: ste-marie) */
    @NotBlank(message = "Le slug est obligatoire")
    private String slug;

    /** Email de contact principal et login du directeur */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    /** Numéro de téléphone de l'école */
    private String phone;

    /** Ville de l'établissement */
    private String city;

    /** Code pays (ex: GN) */
    private String countryCode = "GN";
    
    /** Code du plan d'abonnement choisi (ex: BASIC) */
    @NotBlank(message = "Le plan est obligatoire")
    private String planCode;

    /** Prénom du premier administrateur (Directeur) */
    @NotBlank(message = "Le prénom du directeur est obligatoire")
    private String directorFirstName;

    /** Nom de famille du premier administrateur (Directeur) */
    @NotBlank(message = "Le nom du directeur est obligatoire")
    private String directorLastName;

    /** Mot de passe du compte directeur */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
    private String directorPassword;

    /** Fuseau horaire de l'école */
    private String timezone = "Africa/Conakry";

    /** Devise de l'école */
    private String currency = "GNF";
}

package com.schoolsaas.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnboardingRequest {
    @NotBlank(message = "Le nom de l'école est obligatoire")
    private String schoolName;

    @NotBlank(message = "Le slug est obligatoire")
    private String slug;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    private String phone;
    private String city;
    private String countryCode = "GN";
    
    @NotBlank(message = "Le plan est obligatoire")
    private String planCode;

    @NotBlank(message = "Le prénom du directeur est obligatoire")
    private String directorFirstName;

    @NotBlank(message = "Le nom du directeur est obligatoire")
    private String directorLastName;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
    private String directorPassword;

    private String timezone = "Africa/Conakry";
    private String currency = "GNF";
}

package com.schoolsaas.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Changement d'école, une fois déjà authentifié. Aucun mot de passe requis. */
@Data
public class SwitchSchoolRequest {

    @NotBlank(message = "Le slug de l'école est obligatoire")
    private String schoolSlug;
}

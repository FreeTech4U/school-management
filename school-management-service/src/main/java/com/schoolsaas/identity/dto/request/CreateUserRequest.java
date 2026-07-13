package com.schoolsaas.identity.dto.request;

import com.schoolsaas.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    private String phone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
    private String password;

    @NotNull(message = "Le rôle est obligatoire")
    private Role role; // DIRECTOR, TEACHER, ACCOUNTANT

    private String avatarUrl;

    // Fields for Teacher if role is TEACHER
    private String employeeNumber;
    private LocalDate hireDate;
    private String specialty;
    private String qualification;
    private String bio;
}

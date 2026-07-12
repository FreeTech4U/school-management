package com.schoolsaas.identity.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Représente un utilisateur au sein d'une école.
 * Un utilisateur est toujours rattaché à un tenant spécifique via le schéma de la base de données.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    /** Prénom de l'utilisateur */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /** Nom de famille */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /** Email unique (utilisé comme identifiant de connexion) */
    @Column(unique = true, nullable = false)
    private String email;

    /** Numéro de téléphone */
    private String phone;

    /** Hash du mot de passe (BCrypt) */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Rôle de l'utilisateur (ex: DIRECTOR, TEACHER, ACCOUNTANT, PARENT) */
    @Column(nullable = false)
    private String role; // DIRECTOR, TEACHER, ACCOUNTANT, PARENT

    /** URL vers la photo de profil */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /** Indique si le compte est actif */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /** Date et heure de la dernière connexion réussie */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}

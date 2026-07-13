package com.schoolsaas.identity.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Utilisateur de l'application (personnel de l'école).
 *
 * Un utilisateur appartient toujours à un tenant : il vit dans le schema
 * PostgreSQL de son école. L'isolation est assurée par Hibernate via le
 * TenantContext — aucune colonne school_id n'est nécessaire.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Identifiant de connexion. Unique DANS le tenant. */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    /** Hash BCrypt (force 12). Jamais exposé dans un DTO. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** DIRECTOR · TEACHER · ACCOUNTANT · PARENT */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /** Désactivation = soft delete. Un user inactif ne peut plus se connecter. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** CORRECTION : Instant (colonne TIMESTAMPTZ), et non LocalDateTime. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Nom complet, utilisé dans les DTO et les SMS. */
    @Transient
    public String getFullName() {
        return lastName + " " + firstName;
    }
}


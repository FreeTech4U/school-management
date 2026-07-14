package com.schoolsaas.identity.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Utilisateur de l'application (personnel de l'école).
 *
 * CORRECTION D'ARCHITECTURE — cette entité ne porte plus AUCUNE référence à
 * Role, ni directe (ancien enum Java) ni via association JPA (@ManyToMany
 * tenté puis retiré). Role vit dans platform/ (schema public), un domaine
 * différent d'identity/ (schema tenant) : la frontière se traverse par UUID,
 * jamais par relation JPA — voir UserRoleAssignment pour le détail complet
 * de ce raisonnement.
 *
 * Pour connaître les rôles d'un utilisateur : passer par
 * UserRoleAssignmentRepository.findRoleIdsByUserId(), puis résoudre les codes
 * via platform/RoleCatalogService si nécessaire (fait par AuthTenantService
 * à la connexion, par exemple).
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

    @Column(name = "avatar_url")
    private String avatarUrl;

    /** Désactivation = soft delete. Un user inactif ne peut plus se connecter. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Transient
    public String getFullName() {
        return lastName + " " + firstName;
    }
}


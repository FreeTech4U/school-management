package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Appartenance d'une personne à une école, avec un résumé de ses rôles.
 *
 * ⚠ Schema PUBLIC.
 *
 * Le rôle réel n'est PAS ici : il vit dans <schema_tenant>.user_roles,
 * propre à chaque école. rolesSnapshot n'est qu'un résumé d'affichage
 * dénormalisé, pour peupler un sélecteur d'écoles sans interroger chaque
 * tenant.
 *
 * tenantUserId est un UUID — pas une @ManyToOne vers User — car User vit
 * dans identity/, à l'intérieur d'un schema tenant dynamique.
 */
@Entity
@Table(
        name = "school_memberships",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_person_school",
                columnNames = {"person_id", "school_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolMembership extends BaseEntity {

    /** INTRA-domaine (platform → platform) : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** INTRA-domaine (platform → platform) : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /** INTER-schema (public → tenant) : UUID uniquement. */
    @Column(name = "tenant_user_id", nullable = false)
    private UUID tenantUserId;

    /** Résumé d'affichage, ex : "DIRECTOR, TEACHER". Ne fait pas foi pour les droits. */
    @Column(name = "roles_snapshot", length = 200)
    private String rolesSnapshot;

    /** Coupe-circuit propre à CETTE école, distinct de School.status. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

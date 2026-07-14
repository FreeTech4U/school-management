package com.schoolsaas.identity.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Attribution d'un rôle à un utilisateur, DANS ce tenant.
 *
 * CORRECTION D'ARCHITECTURE — cette classe remplace un @ManyToMany direct
 * entre User et Role qui violait la règle de frontière de domaine : Role vit
 * dans platform/ (schema public), User vit dans identity/ (schema tenant) —
 * deux domaines différents. Le fait que PostgreSQL autorise une clé étrangère
 * inter-schema est une question TECHNIQUE, indépendante de la question
 * ARCHITECTURALE : une frontière de domaine se traverse par UUID, jamais par
 * association JPA, exactement comme ClassSubject.teacherId (academic →
 * identity) ou StudentFee.enrollmentId (finance → enrollment) le font déjà
 * ailleurs dans ce projet.
 *
 * user  : @ManyToOne — INTRA-domaine (identity → identity), légitime.
 * roleId : UUID       — INTER-domaine (identity → platform), jamais d'entité.
 *
 * Pour obtenir le CODE du rôle (ex: "DIRECTOR") à partir de ce roleId, le
 * domaine identity/ appelle le SERVICE du domaine platform/
 * (RoleCatalogService), jamais une requête directe joignant public.roles.
 *
 * CORRECTION SECONDAIRE — la table user_roles avait une clé primaire
 * composite (user_id, role_id), incohérente avec la convention du reste du
 * schéma (class_subjects, payment_allocations...), qui utilise toujours un id
 * surrogate + une contrainte UNIQUE. Cette entité, en étendant BaseEntity
 * comme toutes les autres, suit désormais la même convention.
 */
@Entity
@Table(
    name = "user_roles",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_role",
        columnNames = {"user_id", "role_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserRoleAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;
}

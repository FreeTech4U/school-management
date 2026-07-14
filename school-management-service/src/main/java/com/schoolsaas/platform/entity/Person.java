package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Identité transverse d'une personne, à travers toutes ses écoles.
 *
 * ⚠ Schema PUBLIC. NE CONTIENT AUCUN MOT DE PASSE.
 *
 * L'authentification reste entièrement portée par <schema_tenant>.users
 * (password_hash), une fois par école. Cette table répond uniquement à :
 * « à quelles écoles cet email a-t-il accès, laquelle a-t-il visitée en
 * dernier ? ».
 */
@Entity
@Table(name = "persons", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Person extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Pilote la redirection automatique après connexion (F-02).
     * INTRA-domaine (Person et School vivent toutes deux dans platform/,
     * schema public) : @ManyToOne autorisé et pratique.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_connected_school_id")
    private School lastConnectedSchool;
}

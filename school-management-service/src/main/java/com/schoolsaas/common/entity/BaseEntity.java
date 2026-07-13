package com.schoolsaas.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Classe de base de toutes les entités JPA.
 *
 * CORRECTION MAJEURE — createdAt / updatedAt passent de LocalDateTime à Instant.
 *
 *   Les colonnes SQL sont de type TIMESTAMPTZ (horodatage absolu).
 *   LocalDateTime n'a PAS de fuseau : Hibernate lui applique implicitement celui
 *   de la JVM. Un enregistrement créé à 15h sur le poste de dev (Europe/Paris)
 *   puis relu depuis le VPS (UTC) affichait une heure différente.
 *
 *   Instant est un point absolu sur la ligne du temps : aucune conversion,
 *   aucune ambiguïté, quel que soit le serveur.
 *
 * NOTE : updated_at est AUSSI maintenu par un trigger PostgreSQL
 * (fn_update_updated_at). Les deux mécanismes coexistent sans conflit :
 * l'audit Spring pose la valeur, le trigger la garantit même pour les UPDATE
 * effectués hors JPA (script SQL, migration Flyway).
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Égalité fondée sur l'identifiant.
     *
     * Ne JAMAIS utiliser @EqualsAndHashCode de Lombok sur une entité JPA : il
     * compare tous les champs, ce qui déclenche le chargement des associations
     * LAZY (LazyInitializationException) et casse le comportement des Set<>.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
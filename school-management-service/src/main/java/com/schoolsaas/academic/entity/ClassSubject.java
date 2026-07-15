package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Affectation d'une matière à une classe, avec son enseignant et son coefficient.
 *
 * Le coefficient sert au calcul de la moyenne générale (F-16) :
 *   moyenne = Σ(moyenne_matière × coefficient) / Σ(coefficients)
 *
 * DEUX CORRECTIONS IMPORTANTES :
 *
 * 1. teacher passe d'une @ManyToOne<User> à un UUID.
 *    User appartient au domaine identity/. Une @ManyToOne créait une dépendance
 *    academic → identity, interdite par la règle d'architecture. Pour obtenir le
 *    nom de l'enseignant, le service passe par UserService.
 *
 * 2. teacherId devient NULLABLE, et la validation @PrePersist qui levait une
 *    exception quand teacher était null a été supprimée.
 *    Les specs (F-07) prévoient explicitement qu'une matière puisse être créée
 *    AVANT qu'un enseignant y soit affecté. Le code précédent rendait ce cas
 *    impossible, en contradiction directe avec la colonne SQL (nullable) et avec
 *    le ON DELETE SET NULL de la clé étrangère.
 *
 * Les validations de plage (coefficient entre 1 et 10) sont assurées par les
 * contraintes CHECK en base et par la Bean Validation sur les DTO d'entrée.
 * Les dupliquer dans un @PrePersist qui lève des IllegalArgumentException est
 * redondant et produit des erreurs 500 au lieu d'erreurs 400 explicites.
 */
@Entity
@Table(
        name = "class_subjects",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_class_subject",
                columnNames = {"class_id", "subject_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClassSubject extends BaseEntity {

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /**
     * INTER-domaine (academic → identity) : UUID, pas d'association JPA.
     * NULLABLE : la matière peut exister avant l'affectation d'un enseignant.
     */
    @Column(name = "teacher_id")
    private UUID teacherId;

    /** Entre 1 et 10 (CHECK en base). */
    @Column(nullable = false)
    @Builder.Default
    private Short coefficient = 1;

    @Column(name = "weekly_hours")
    private Short weeklyHours;
}


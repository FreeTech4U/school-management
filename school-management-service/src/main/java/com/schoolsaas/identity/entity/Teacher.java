package com.schoolsaas.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Informations complémentaires propres aux enseignants.
 *
 * Relation 1-1 STRICTE avec User : la clé primaire de teachers EST la clé
 * primaire de users (@MapsId). Une ligne est créée automatiquement par
 * UserService dès qu'un utilisateur est créé avec le rôle TEACHER (F-03).
 *
 * POURQUOI CETTE CLASSE N'ÉTEND PAS BaseEntity :
 *   BaseEntity porte @GeneratedValue(strategy = UUID), qui demande à Hibernate
 *   de générer l'identifiant. Or ici l'identifiant est IMPOSÉ par User via
 *   @MapsId. Les deux mécanismes sont incompatibles : Hibernate lèverait une
 *   erreur au démarrage.
 *   L'id et les timestamps sont donc redéclarés localement.
 */
@Entity
@Table(name = "teachers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Teacher {

    /** Même valeur que User.id — renseignée par @MapsId. */
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "employee_number", unique = true, length = 50)
    private String employeeNumber;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(length = 100)
    private String specialty;

    @Column(length = 255)
    private String qualification;

    @Column(columnDefinition = "TEXT")
    private String bio;

    /** CORRECTION : Instant (colonnes TIMESTAMPTZ), et non LocalDateTime. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

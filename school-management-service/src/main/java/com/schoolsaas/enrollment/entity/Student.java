package com.schoolsaas.enrollment.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Fiche élève.
 *
 * MATRICULE : studentNumber est généré par un trigger PostgreSQL au format
 * EL-YYYY-NNNN. Ne JAMAIS le renseigner depuis le code — le trigger ne se
 * déclenche que si la valeur est nulle.
 *
 * PAS DE CHAMP « status » — divergence assumée avec le diagramme de classes.
 *   Le diagramme prévoyait un StudentStatus (ACTIVE / LEFT / GRADUATED). Ce
 *   statut est une donnée DÉRIVÉE de la dernière inscription de l'élève :
 *   StudentEnrollment.status porte déjà WITHDRAWN et GRADUATED.
 *   Le dupliquer ici créerait deux sources de vérité contradictoires. Cas qui
 *   casse le modèle : un élève parti en 2024 (LEFT) qui se réinscrit en 2026 —
 *   que vaudrait alors students.status, sachant que son inscription de 2024
 *   reste WITHDRAWN ?
 *   isActive ci-dessous est un simple drapeau de soft delete, besoin technique
 *   distinct du parcours scolaire.
 */
@Entity
@Table(name = "students")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Student extends BaseEntity {

    /** EL-YYYY-NNNN. Généré par le trigger trg_generate_student_number. */
    @Column(name = "student_number", unique = true, length = 20)
    private String studentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * CORRECTION : NULLABLE (le code précédent imposait nullable = false).
     * En Guinée, une inscription se fait couramment sans que la famille dispose
     * de tous les documents. Rendre ce champ obligatoire forçait les comptables
     * à saisir des dates fausses.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * CORRECTION : enum Gender au lieu d'un String de longueur 1, et NULLABLE.
     * Le mapping vers la colonne CHAR(1) ('M' / 'F') est assuré par
     * GenderConverter (autoApply), et non par @Enumerated qui écrirait "MALE".
     */
    @Column(length = 1)
    private Gender gender;

    @Column(name = "birth_city", length = 100)
    private String birthCity;

    @Column(name = "birth_country", length = 3)
    @Builder.Default
    private String birthCountry = "GN";

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "parent_name", length = 255)
    private String parentName;

    /** Format international +224XXXXXXXXX. Destinataire de tous les SMS. */
    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Column(name = "medical_notes", columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Transient
    public String getFullName() {
        return lastName + " " + firstName;
    }
}

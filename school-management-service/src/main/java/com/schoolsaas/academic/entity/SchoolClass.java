package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Classe (ex : « 6ème A »).
 *
 * NOMMAGE : l'entité s'appelle SchoolClass et non Class, « class » étant un mot
 * réservé du langage Java. La table, elle, reste « classes ».
 *
 * CORRECTION — academicYear passe d'un UUID à une @ManyToOne.
 * AcademicYear appartient au MÊME domaine (academic/) : l'association JPA est
 * non seulement autorisée, mais préférable — elle donne la navigation et la
 * cohérence référentielle. Le code précédent mélangeait les deux stratégies
 * sans logique (academicYearId en UUID, level en @ManyToOne, alors que les deux
 * sont dans academic/).
 */
@Entity
@Table(
        name = "classes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_class_name_per_year",
                columnNames = {"academic_year_id", "name"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass extends BaseEntity {

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "level_id", nullable = false)
    private Level level;

    /** Ex : "6ème A". Unique par année scolaire. */
    @Column(nullable = false, length = 100)
    private String name;

    /** Filière : Scientifique, Littéraire... */
    @Column(length = 100)
    private String option;

    /** Effectif maximum. NULL = non limité. */
    private Short capacity;

    @Column(name = "room_number", length = 20)
    private String roomNumber;
}


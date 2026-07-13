package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Matière enseignée (Mathématiques, Français...).
 *
 * La suppression est un soft delete : isActive = false. Une matière ne peut pas
 * être réellement supprimée car des notes historiques y font référence.
 */
@Entity
@Table(name = "subjects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Code court, ex : "MATH". */
    @Column(unique = true, length = 20)
    private String code;

    /** Couleur d'affichage dans l'emploi du temps, format #RRGGBB. */
    @Column(length = 7)
    private String color;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

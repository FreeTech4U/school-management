package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Niveau du cursus : Primaire, Collège, Lycée.
 *
 * orderIndex définit l'ordre de progression, utilisé par le calcul de promotion
 * (F-19) pour déterminer la classe suivante — et donc si un élève atteint la fin
 * du cycle (statut GRADUATED).
 */
@Entity
@Table(name = "levels")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Level extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "order_index", nullable = false, unique = true)
    private Short orderIndex;
}

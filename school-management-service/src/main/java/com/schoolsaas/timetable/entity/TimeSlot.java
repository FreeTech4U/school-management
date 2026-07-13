package com.schoolsaas.timetable.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.DayOfWeek;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Créneau horaire hebdomadaire, ex : lundi 08h00–09h00 (F-18).
 *
 * ⚠ DayOfWeek est l'enum du PROJET (com.schoolsaas.common.enums), à ne pas
 *   confondre avec java.time.DayOfWeek. Deux raisons :
 *     • java.time.DayOfWeek inclut SUNDAY, or les écoles guinéennes n'ont pas
 *       cours le dimanche : autoriser cette valeur permettrait la saisie d'un
 *       créneau aberrant.
 *     • Un enum du JDK ne peut pas porter de libellé français.
 *
 * LocalTime est le bon type pour une colonne TIME : une heure de cours n'a pas
 * de fuseau, c'est voulu.
 */
@Entity
@Table(
        name = "time_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_time_slot",
                columnNames = {"day_of_week", "start_time", "end_time"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot extends BaseEntity {

    /** MONDAY .. SATURDAY (SUNDAY volontairement exclu). */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Ex : "Heure 1", "Pause déjeuner". */
    @Column(length = 30)
    private String label;

    /** Ordre d'affichage dans la grille. NOT NULL en base. */
    @Column(name = "order_index", nullable = false)
    private Short orderIndex;
}

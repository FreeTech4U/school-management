package com.schoolsaas.timetable.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Placement d'une matière dans la grille hebdomadaire (F-18).
 *
 * DÉTECTION DES CONFLITS — deux règles NE PEUVENT PAS être exprimées par une
 * contrainte SQL, car elles nécessitent une jointure vers class_subjects :
 *   • une classe ne peut pas suivre deux matières sur le même créneau
 *     → CLASS_TIMESLOT_CONFLICT
 *   • un enseignant ne peut pas être à deux endroits en même temps
 *     → TEACHER_TIMESLOT_CONFLICT
 * Elles sont vérifiées par TimetableService.validateNoConflict().
 *
 * La contrainte UNIQUE en base ne couvre que le cas trivial : placer deux fois
 * la MÊME matière sur le MÊME créneau.
 */
@Entity
@Table(
        name = "timetable_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_timetable_entry",
                columnNames = {"class_subject_id", "time_slot_id", "academic_year_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TimetableEntry extends BaseEntity {

    /** INTER-domaine (timetable → academic) : UUID. */
    @Column(name = "class_subject_id", nullable = false)
    private UUID classSubjectId;

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    /** INTER-domaine (timetable → academic) : UUID. */
    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    /** INTER-domaine (timetable → academic) : UUID. NULL = toute l'année. */
    @Column(name = "term_id")
    private UUID termId;

    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

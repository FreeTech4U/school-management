package com.schoolsaas.timetable.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "timetable_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntry extends BaseEntity {

    @Column(name = "class_subject_id", nullable = false)
    private UUID classSubjectId;

    @Column(name = "time_slot_id", nullable = false)
    private UUID timeSlotId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "term_id")
    private UUID termId; // null = whole year

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "is_active")
    private Boolean isActive = true;
}

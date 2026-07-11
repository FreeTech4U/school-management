package com.schoolsaas.timetable.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "time_slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot extends BaseEntity {

    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek; // MONDAY..SATURDAY

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    private String label;

    @Column(name = "order_index")
    private Integer orderIndex;
}

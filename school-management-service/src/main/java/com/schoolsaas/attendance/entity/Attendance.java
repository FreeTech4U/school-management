package com.schoolsaas.attendance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends BaseEntity {

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String period = "FULL_DAY"; // FULL_DAY, MORNING, AFTERNOON

    @Column(nullable = false)
    private String status; // PRESENT, ABSENT, LATE, EXCUSED

    private String justification;

    @Column(name = "recorded_by")
    private UUID recordedBy;
}

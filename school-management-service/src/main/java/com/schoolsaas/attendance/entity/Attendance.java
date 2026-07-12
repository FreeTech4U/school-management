package com.schoolsaas.attendance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.AttendanceStatus;
import com.schoolsaas.common.enums.Period;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Period period = Period.FULL_DAY;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    private String justification;

    @Column(name = "recorded_by")
    private UUID recordedBy;
}

package com.schoolsaas.academic.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "academic_years")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current")
    private Boolean isCurrent = false;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, CLOSED
}

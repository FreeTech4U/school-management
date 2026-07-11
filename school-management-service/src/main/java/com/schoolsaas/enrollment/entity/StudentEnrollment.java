package com.schoolsaas.enrollment.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollment extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate = LocalDate.now();

    @Column(name = "is_repeating")
    private Boolean isRepeating = false;

    @Column(nullable = false)
    private String status = "ENROLLED"; // ENROLLED, TRANSFERRED, WITHDRAWN, GRADUATED

    @Column(name = "promotion_status")
    private String promotionStatus = "PENDING"; // PENDING, PROMOTED, REPEATED, GRADUATED

    @Column(name = "final_average")
    private BigDecimal finalAverage;
}

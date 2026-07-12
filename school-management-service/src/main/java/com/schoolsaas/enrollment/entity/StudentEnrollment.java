package com.schoolsaas.enrollment.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.EnrollmentStatus;
import com.schoolsaas.common.enums.PromotionStatus;
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
    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "promotion_status")
    @Enumerated(EnumType.STRING)
    private PromotionStatus promotionStatus = PromotionStatus.PENDING;

    @Column(name = "final_average")
    private BigDecimal finalAverage;

    @Column(name = "transfer_notes")
    private String transferNotes;
}

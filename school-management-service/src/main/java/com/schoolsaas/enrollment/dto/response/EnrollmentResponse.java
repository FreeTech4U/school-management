package com.schoolsaas.enrollment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class EnrollmentResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentNumber;
    private UUID classId;
    private String className;
    private UUID academicYearId;
    private String academicYearLabel;
    private LocalDate enrollmentDate;
    private Boolean isRepeating;
    private String status;
    private String promotionStatus;
    private BigDecimal finalAverage;
}

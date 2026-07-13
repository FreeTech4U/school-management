package com.schoolsaas.grading.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GradeResponse {
    private UUID id;
    private UUID enrollmentId;
    private UUID classSubjectId;
    private UUID termId;
    private BigDecimal value;
    private String evaluationType;
    private String evaluationLabel;
    private LocalDate evaluationDate;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

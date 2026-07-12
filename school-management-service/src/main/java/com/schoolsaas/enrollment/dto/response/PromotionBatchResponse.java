package com.schoolsaas.enrollment.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionBatchResponse {

    private UUID id;
    private UUID classId;
    private UUID academicYearId;
    private UUID nextAcademicYearId;
    private String status; // CREATED, VALIDATED, EXECUTED, CANCELLED
    private Integer promotedCount;
    private Integer repeatedCount;
    private Integer graduatedCount;
    private Integer totalProcessed;
    private String validationErrors;
    private LocalDateTime executedAt;
    private String notes;
    private String directorComment;
    private LocalDateTime createdAt;
}

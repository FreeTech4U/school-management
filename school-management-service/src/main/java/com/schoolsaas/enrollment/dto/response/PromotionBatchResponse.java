package com.schoolsaas.enrollment.dto.response;

import com.schoolsaas.common.enums.PromotionBatchStatus;
import lombok.*;

import java.time.Instant;
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
    private PromotionBatchStatus status; // CREATED, VALIDATED, EXECUTED, CANCELLED
    private Integer promotedCount;
    private Integer repeatedCount;
    private Integer graduatedCount;
    private Integer totalProcessed;
    private String validationErrors;
    private Instant executedAt;
    private String notes;
    private String directorComment;
    private Instant createdAt;
}

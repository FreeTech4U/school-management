package com.schoolsaas.enrollment.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePromotionBatchRequest {

    @NotNull(message = "Class ID is required")
    private UUID classId;

    @NotNull(message = "Current academic year ID is required")
    private UUID academicYearId;

    @NotNull(message = "Next academic year ID is required")
    private UUID nextAcademicYearId;

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    private String notes;
}

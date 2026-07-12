package com.schoolsaas.grading.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReportCardsRequest {

    @NotNull(message = "Class ID is required")
    private UUID classId;

    @NotNull(message = "Term ID is required")
    private UUID termId;
}

package com.schoolsaas.grading.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkGradeRequest {
    
    @NotNull(message = "classSubjectId is required")
    private UUID classSubjectId;
    
    @NotNull(message = "termId is required")
    private UUID termId;
    
    @NotBlank(message = "evaluationType is required")
    private String evaluationType;
    
    @NotBlank(message = "evaluationLabel is required")
    @Size(min = 1, max = 100)
    private String evaluationLabel;
    
    @NotNull(message = "evaluationDate is required")
    private LocalDate evaluationDate;
    
    @Valid
    @NotEmpty(message = "grades list cannot be empty")
    private List<BulkGradeItem> grades;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkGradeItem {
        @NotNull(message = "enrollmentId is required")
        private UUID enrollmentId;
        
        @NotNull(message = "value is required")
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "20.0")
        private BigDecimal value;
        
        @Size(max = 500)
        private String comment;
    }
}

package com.schoolsaas.finance.dto.request;

import com.schoolsaas.common.enums.FeeType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureRequest {

    @NotNull(message = "Academic year ID is required")
    private java.util.UUID academicYearId;

    private java.util.UUID classId; // null = applies to all classes

    @NotNull(message = "Fee type is required")
    private FeeType feeType;

    @NotBlank(message = "Label is required")
    @Size(min = 3, max = 100, message = "Label must be between 3 and 100 characters")
    private String label;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Installments allowed flag is required")
    private Boolean installmentsAllowed = false;

    @Min(value = 1, message = "Max installments must be at least 1")
    @Max(value = 12, message = "Max installments cannot exceed 12")
    private Short maxInstallments = 3;
}

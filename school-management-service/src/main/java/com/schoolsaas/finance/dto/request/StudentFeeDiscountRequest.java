package com.schoolsaas.finance.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeDiscountRequest {

    @NotNull(message = "Discount amount is required")
    @DecimalMin(value = "0", message = "Discount amount cannot be negative")
    private BigDecimal discountAmount;

    @Size(max = 255, message = "Discount reason cannot exceed 255 characters")
    private String discountReason;
}

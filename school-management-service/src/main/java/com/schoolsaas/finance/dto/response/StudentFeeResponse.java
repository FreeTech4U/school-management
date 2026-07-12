package com.schoolsaas.finance.dto.response;

import com.schoolsaas.common.enums.FeeStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeResponse {

    private UUID id;
    private UUID enrollmentId;
    private UUID feeStructureId;
    private String feeLabel; // from feeStructure
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private BigDecimal discountAmount;
    private String discountReason;
    private LocalDate dueDate;
    private FeeStatus status;
    private LocalDateTime createdAt;
}

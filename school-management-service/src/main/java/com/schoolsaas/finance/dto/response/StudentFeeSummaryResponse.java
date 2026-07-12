package com.schoolsaas.finance.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFeeSummaryResponse {

    private UUID enrollmentId;
    private String studentName;
    private BigDecimal totalDue;
    private BigDecimal totalPaid;
    private BigDecimal totalDiscount;
    private BigDecimal amountRemaining;
    private Integer unpaidFeeCount;
    private Integer paidFeeCount;
    private String overallStatus; // ALL_PAID, PARTIAL, UNPAID, OVERDUE
}

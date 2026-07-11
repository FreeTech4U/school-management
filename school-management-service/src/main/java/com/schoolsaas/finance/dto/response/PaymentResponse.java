package com.schoolsaas.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String referenceNumber;
    private String receiptNumber;
    private List<AllocationResponse> allocations;

    @Data
    @Builder
    public static class AllocationResponse {
        private UUID studentFeeId;
        private String feeLabel;
        private BigDecimal amountAllocated;
    }
}

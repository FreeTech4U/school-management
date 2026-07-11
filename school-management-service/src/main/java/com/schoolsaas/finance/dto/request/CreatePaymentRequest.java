package com.schoolsaas.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreatePaymentRequest {
    @NotNull(message = "L'élève est obligatoire")
    private UUID studentId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal amount;

    @NotBlank(message = "Le mode de paiement est obligatoire")
    private String paymentMethod;

    private String referenceNumber;
    private LocalDate paymentDate = LocalDate.now();
    private String notes;

    @NotEmpty(message = "Au moins une allocation est obligatoire")
    private List<AllocationRequest> allocations;

    @Data
    public static class AllocationRequest {
        @NotNull(message = "Le frais est obligatoire")
        private UUID studentFeeId;
        
        @NotNull(message = "Le montant alloué est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant alloué doit être supérieur à 0")
        private BigDecimal amount;
    }
}

package com.schoolsaas.finance.dto.response;

import com.schoolsaas.common.enums.FeeType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureResponse {

    private UUID id;
    private UUID academicYearId;
    private UUID classId;
    private FeeType feeType;
    private String label;
    private BigDecimal amount;
    private LocalDate dueDate;
    private Boolean installmentsAllowed;
    private Short maxInstallments;
    private Instant createdAt;
    private Instant updatedAt;
}

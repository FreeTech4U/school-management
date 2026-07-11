package com.schoolsaas.finance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "receipt_number", unique = true)
    private String receiptNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // CASH, ORANGE_MONEY, MTN_MONEY, WAVE, BANK_TRANSFER, CHECK

    @Column(name = "reference_number")
    private String referenceNumber;

    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private List<PaymentAllocation> allocations;
}

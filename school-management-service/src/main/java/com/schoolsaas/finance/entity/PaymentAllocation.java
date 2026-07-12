package com.schoolsaas.finance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_allocations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_fee_id", nullable = false)
    private StudentFee studentFee;

    @Column(nullable = false)
    private BigDecimal amount;

    @PrePersist
    @PreUpdate
    private void validateAllocation() {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Allocation amount must be positive");
        }
        if (studentFee == null) {
            throw new IllegalArgumentException("Student fee is required");
        }
        if (payment == null) {
            throw new IllegalArgumentException("Payment is required");
        }
    }
}

package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_payments", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SchoolSubscription subscription;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "GNF";

    @Column(name = "payment_date")
    private LocalDateTime paymentDate = LocalDateTime.now();

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    @Column(nullable = false)
    private String status = "completed";
}

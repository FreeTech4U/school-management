package com.schoolsaas.finance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_fees")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFee extends BaseEntity {

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    @Column(name = "amount_due", nullable = false)
    private BigDecimal amountDue;

    @Column(name = "amount_paid")
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_reason")
    private String discountReason;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String status = "UNPAID"; // UNPAID, PARTIAL, PAID, OVERDUE, WAIVED

    @Column(name = "last_reminder_sent_at")
    private LocalDateTime lastReminderSentAt;
}

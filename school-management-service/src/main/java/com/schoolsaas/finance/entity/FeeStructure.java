package com.schoolsaas.finance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_structures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructure extends BaseEntity {

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "class_id")
    private UUID classId; // null = all classes

    @Column(name = "fee_type", nullable = false)
    private String feeType; // TUITION, REGISTRATION, CANTEEN, TRANSPORT, EXAM

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "installments_allowed")
    private Boolean installmentsAllowed = false;

    @Column(name = "max_installments")
    private Integer maxInstallments = 3;
}

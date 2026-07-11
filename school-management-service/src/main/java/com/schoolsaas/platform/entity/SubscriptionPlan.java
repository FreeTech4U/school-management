package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "subscription_plans", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private java.math.BigDecimal price;

    @Column(length = 3)
    private String currency = "GNF";

    @Column(name = "billing_cycle")
    private String billingCycle = "MONTHLY";

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "is_active")
    private Boolean isActive = true;
}

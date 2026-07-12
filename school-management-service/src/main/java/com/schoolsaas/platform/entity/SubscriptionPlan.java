package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Définit un plan d'abonnement disponible sur la plateforme (ex: Basic, Pro, Enterprise).
 */
@Entity
@Table(name = "subscription_plans", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan extends BaseEntity {

    /** Code unique du plan (ex: BASIC_MONTHLY) */
    @Column(unique = true, nullable = false)
    private String code;

    /** Nom affiché du plan */
    @Column(nullable = false)
    private String name;

    /** Description détaillée des fonctionnalités incluses */
    private String description;

    /** Prix du plan pour le cycle de facturation */
    @Column(nullable = false)
    private java.math.BigDecimal price;

    /** Devise du prix (ex: GNF) */
    @Column(length = 3)
    private String currency = "GNF";

    /** Cycle de facturation (MONTHLY, YEARLY) */
    @Column(name = "billing_cycle")
    private String billingCycle = "MONTHLY";

    /** Nombre maximum d'élèves autorisés pour ce plan */
    @Column(name = "max_students")
    private Integer maxStudents;

    /** Indique si le plan est actuellement disponible à la souscription */
    @Column(name = "is_active")
    private Boolean isActive = true;
}

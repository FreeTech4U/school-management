package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.BillingCycle;
import com.schoolsaas.common.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abonnement souscrit par une école.
 *
 * ⚠ Schema PUBLIC.
 *
 * UNICITÉ : une école ne peut avoir qu'UN SEUL abonnement ACTIVE à la fois.
 * Garanti en base par un index UNIQUE partiel (uq_school_active_subscription).
 * Sans cette contrainte, un renouvellement interrompu ou un double-clic créait
 * deux abonnements actifs — et le plan appliqué devenait non déterministe.
 *
 * BILLING CYCLE : porté par l'ABONNEMENT, et non par le plan. C'est lui qui
 * détermine lequel des deux tarifs du plan s'applique.
 *
 * AJOUTS : smsUsedThisMonth et smsCounterResetAt.
 *   Le quota sms_included existait sur le plan, mais AUCUN compteur ne mesurait
 *   la consommation réelle : le quota était donc inapplicable — impossible de
 *   bloquer un dépassement ou de facturer un surplus.
 */
@Entity
@Table(name = "school_subscriptions", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSubscription extends BaseEntity {

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    /** MONTHLY (1 mois) · YEARLY (12 mois, mode privilégié). */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.YEARLY;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** ACTIVE · EXPIRED · CANCELLED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = true;

    /** AJOUT — compteur de SMS consommés sur le mois en cours. */
    @Column(name = "sms_used_this_month", nullable = false)
    @Builder.Default
    private Integer smsUsedThisMonth = 0;

    /** AJOUT — date de la dernière remise à zéro du compteur. */
    @Column(name = "sms_counter_reset_at", nullable = false)
    @Builder.Default
    private LocalDate smsCounterResetAt = LocalDate.now();

    /** Montant à facturer, selon le cycle choisi. */
    @Transient
    public BigDecimal getBillingAmount() {
        return billingCycle == BillingCycle.YEARLY
                ? plan.getPriceYearly()
                : plan.getPriceMonthly();
    }

    /** Quota SMS dépassé ? */
    @Transient
    public boolean isSmsQuotaExceeded() {
        return smsUsedThisMonth >= plan.getSmsIncluded();
    }
}

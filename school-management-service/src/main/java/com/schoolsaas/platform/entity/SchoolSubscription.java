package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Gère l'association entre une école et un plan d'abonnement.
 * Définit la période de validité et le statut de l'accès aux services.
 */
@Entity
@Table(name = "school_subscriptions", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSubscription extends BaseEntity {

    /** École concernée par l'abonnement */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    /** Plan de souscription choisi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    /** Date de début de validité */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Date de fin de validité */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Statut de l'abonnement (active, expired, cancelled) */
    @Column(nullable = false)
    private String status = "active"; // active, expired, cancelled

    /** Indique si l'abonnement doit se renouveler automatiquement à expiration */
    @Column(name = "auto_renew")
    private Boolean autoRenew = true;
}

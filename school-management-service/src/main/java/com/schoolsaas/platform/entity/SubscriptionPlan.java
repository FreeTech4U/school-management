package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Plan d'abonnement du catalogue SchoolSaaS (Starter · Standard · Premium).
 *
 * ⚠ Schema PUBLIC.
 *
 * CORRECTION MAJEURE — le champ « price » n'existait PAS en base.
 *   L'entité déclarait :  @Column(nullable = false) private BigDecimal price;
 *   Or la table porte price_monthly ET price_yearly. Au démarrage :
 *       ERROR: column "price" of relation "subscription_plans" does not exist
 *
 * POURQUOI DEUX PRIX :
 *   Le modèle précédent portait un billing_cycle SUR LE PLAN, ce qui créait une
 *   ambiguïté irrésolvable : pour un plan YEARLY, price_monthly valait-il le
 *   prix mensuel à multiplier par 12, ou le prix annuel mal nommé ?
 *   Désormais le plan porte les deux tarifs, et c'est l'ABONNEMENT qui porte le
 *   cycle choisi par l'école. Deux écoles peuvent souscrire au même plan, l'une
 *   au mois, l'autre à l'année.
 *
 * AJOUTS : priceYearly, smsIncluded, features (tous présents en base).
 */
@Entity
@Table(name = "subscription_plans", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPlan extends BaseEntity {

    /** starter · standard · premium */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Tarif mensuel, en GNF. */
    @Column(name = "price_monthly", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceMonthly;

    /**
     * Tarif annuel, en GNF.
     * Volontairement inférieur à 12 × priceMonthly (2 mois offerts) : la remise
     * est l'incitation commerciale à l'engagement. Un CHECK en base garantit
     * priceYearly <= priceMonthly * 12.
     */
    @Column(name = "price_yearly", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceYearly;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GNF";

    /** NULL = illimité (plan Premium). */
    @Column(name = "max_students")
    private Integer maxStudents;

    /** Quota mensuel de SMS. Consommation suivie sur SchoolSubscription. */
    @Column(name = "sms_included", nullable = false)
    @Builder.Default
    private Integer smsIncluded = 100;

    /** Fonctionnalités activées : timetable, advanced_reports, parent_app. */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> features = new HashMap<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
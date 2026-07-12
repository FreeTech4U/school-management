package com.schoolsaas.platform.repository;

import com.schoolsaas.platform.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour les plans d'abonnement.
 */
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    /** Récupère un plan par son code unique (ex: BASIC_MONTHLY) */
    Optional<SubscriptionPlan> findByCode(String code);
}

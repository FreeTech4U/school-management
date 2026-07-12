package com.schoolsaas.platform.repository;

import com.schoolsaas.platform.entity.SchoolSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository pour les souscriptions des écoles.
 */
public interface SchoolSubscriptionRepository extends JpaRepository<SchoolSubscription, UUID> {
}

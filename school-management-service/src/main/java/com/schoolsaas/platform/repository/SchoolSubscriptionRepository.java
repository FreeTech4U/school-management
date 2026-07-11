package com.schoolsaas.platform.repository;

import com.schoolsaas.platform.entity.SchoolSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SchoolSubscriptionRepository extends JpaRepository<SchoolSubscription, UUID> {
}

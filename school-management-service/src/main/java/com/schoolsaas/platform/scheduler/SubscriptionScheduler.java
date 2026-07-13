package com.schoolsaas.platform.scheduler;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.common.enums.SubscriptionStatus;
import com.schoolsaas.platform.entity.SchoolSubscription;
import com.schoolsaas.platform.repository.SchoolSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Planificateur de tâches pour la gestion des abonnements.
 * Gère les expirations automatiques et les changements de statut.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SchoolSubscriptionRepository subscriptionRepository;

    /**
     * Vérifie quotidiennement les abonnements arrivés à échéance.
     * Si un abonnement est expiré, le statut de l'école passe en "suspended".
     */
    @Scheduled(cron = "0 0 1 * * *") // Every day at 1 AM
    @Transactional
    public void checkExpirations() {
        log.info("Checking for expired subscriptions...");
        
        List<SchoolSubscription> subscriptions = subscriptionRepository.findAll();
        LocalDate today = LocalDate.now();

        for (SchoolSubscription sub : subscriptions) {
            if (SubscriptionStatus.ACTIVE.equals(sub.getStatus()) && sub.getEndDate().isBefore(today)) {
                log.info("Subscription for school {} expired", sub.getSchool().getName());
                sub.setStatus(SubscriptionStatus.EXPIRED);
                sub.getSchool().setStatus(SchoolStatus.SUSPENDED);
                subscriptionRepository.save(sub);
            }
        }
    }
}

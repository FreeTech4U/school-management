package com.schoolsaas.dashboard.scheduler;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.dashboard.service.DashboardService;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import com.schoolsaas.config.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardScheduler {

    private final DashboardService dashboardService;
    private final SchoolRepository schoolRepository;

    /**
     * Rafraîchit la vue matérialisée du dashboard pour toutes les écoles actives.
     *
     * Le cron est externalisé dans application.yml pour pouvoir l'ajuster
     * sans recompiler (et le désactiver en test avec "-").
     */
    @Scheduled(cron = "${app.scheduler.dashboard-refresh.cron:0 */15 * * * *}")
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public void refreshAllDashboards() {

        // CORRECTION : filtrer en SQL plutôt qu'en Java.
        // findAll() charge TOUTES les écoles en mémoire (y compris
        // les suspended/deleted) juste pour en écarter la plupart ensuite.
        // Avec 500 écoles, c'est 500 lignes chargées pour rien.
        List<School> schools = schoolRepository.findAllByStatusIn(List.of(SchoolStatus.TRIAL, SchoolStatus.ACTIVE));

        log.info("Rafraîchissement des dashboards — {} école(s) active(s)", schools.size());

        int success = 0;
        int failed  = 0;

        for (School school : schools) {
            try {
                TenantContext.set(school.getSchemaName());
                dashboardService.refreshStats();
                success++;

            } catch (Exception e) {
                failed++;
                // Le log inclut le schemaName : indispensable pour débugger
                // quelle école pose problème
                log.error("Échec du refresh pour {} (schema={}): {}",
                        school.getName(), school.getSchemaName(), e.getMessage());

            } finally {
                // Toujours nettoyer, même en cas d'exception :
                // le thread du scheduler est réutilisé entre les exécutions,
                // un TenantContext qui traîne polluerait l'itération suivante
                TenantContext.clear();
            }
        }

        log.info("Dashboards rafraîchis — {} succès, {} échec(s)", success, failed);
    }
}

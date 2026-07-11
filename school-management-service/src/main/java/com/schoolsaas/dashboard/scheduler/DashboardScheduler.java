package com.schoolsaas.dashboard.scheduler;

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

    @Scheduled(cron = "0 */45 * * * *") // Every 15 minutes
    @CacheEvict(value = "dashboard_stats", allEntries = true)
    public void refreshAllDashboards() {
        log.info("Refreshing all dashboard stats...");
        List<School> schools = schoolRepository.findAll();
        
        for (School school : schools) {
            if ("active".equals(school.getStatus()) || "trial".equals(school.getStatus())) {
                try {
                    TenantContext.set(school.getSchemaName());
                    dashboardService.refreshStats();
                } catch (Exception e) {
                    log.error("Failed to refresh dashboard for {}: {}", school.getName(), e.getMessage());
                } finally {
                    TenantContext.clear();
                }
            }
        }
    }
}

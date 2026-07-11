package com.schoolsaas.platform.config;

import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import com.schoolsaas.platform.service.TenantMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInitializer implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final TenantMigrationService migrationService;

    @Override
    public void run(String... args) {
        log.info("Starting database migration for all active tenants...");
        
        List<School> activeSchools = schoolRepository.findAllByStatusIn(Arrays.asList("trial", "active"));
        
        log.info("Found {} active schools to migrate.", activeSchools.size());
        
        for (School school : activeSchools) {
            try {
                migrationService.migrateTenant(school.getSchemaName());
            } catch (Exception e) {
                log.error("Failed to migrate schema for school: {} ({})", 
                        school.getName(), school.getSchemaName(), e);
            }
        }
        
        log.info("Tenant migrations completed.");
    }
}

package com.schoolsaas.platform.config;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import com.schoolsaas.platform.service.TenantMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Composant chargé d'initialiser les schémas de tous les tenants actifs au démarrage de l'application.
 * Garantit que chaque école dispose de la dernière version du schéma de base de données.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInitializer implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final TenantMigrationService migrationService;

    /**
     * Méthode exécutée automatiquement au démarrage.
     * Récupère les écoles actives et applique les migrations Flyway sur leurs schémas respectifs.
     */
    @Override
    public void run(String... args) {
        log.info("Starting database migration for all active tenants...");
        
        List<School> activeSchools = schoolRepository.findAllByStatusIn(Arrays.asList(SchoolStatus.TRIAL, SchoolStatus.ACTIVE));
        
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

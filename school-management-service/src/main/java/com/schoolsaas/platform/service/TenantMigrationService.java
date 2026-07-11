package com.schoolsaas.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMigrationService {

    private final DataSource dataSource;

    public void migrateTenant(String schemaName) {
        log.info("Applying Flyway migrations to schema: {}", schemaName);
        
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration/tenant")
                    .schemas(schemaName)
                    .defaultSchema(schemaName)
                    .baselineOnMigrate(true)
                    .validateOnMigrate(false)
                    .load();
            
            flyway.migrate();
            log.info("Successfully migrated schema: {}", schemaName);
        } catch (Exception e) {
            log.error("Error migrating schema {}: {}", schemaName, e.getMessage());
            throw e;
        }
    }
}

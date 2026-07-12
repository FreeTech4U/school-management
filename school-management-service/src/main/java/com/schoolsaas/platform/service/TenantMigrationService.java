package com.schoolsaas.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Service technique chargé d'appliquer les migrations de base de données (Flyway)
 * sur les schémas spécifiques des écoles (tenants).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMigrationService {

    private final DataSource dataSource;

    /**
     * Applique ou met à jour le schéma de base de données pour un tenant donné.
     * Utilise les scripts situés dans `db/migration/tenant`.
     *
     * @param schemaName Nom du schéma PostgreSQL à migrer.
     */
    public void migrateTenant(String schemaName) {
        log.info("Applying Flyway migrations to schema: {}", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenant")
                .schemas(schemaName)
                .defaultSchema(schemaName)
                /*
                 * FIX 1 — baselineVersion("0")
                 *
                 * Sans ça, baselineOnMigrate=true crée un baseline à la version 1
                 * (valeur par défaut), ce qui marque V1 comme "déjà appliquée"
                 * SANS l'exécuter. En mettant la baseline à "0", V1 sera bien exécutée.
                 */
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false)
                .load();

        /*
         * FIX 2 — repair() avant migrate()
         *
         * Si une exécution précédente a laissé une entrée FAILED dans
         * flyway_schema_history (ex: premier run qui a crashé à cause
         * de fn_update_updated_at() introuvable), Flyway refuse de
         * relancer migrate() sans un repair() préalable.
         *
         * repair() nettoie les entrées FAILED et recalcule les checksums.
         */
        flyway.repair();
        flyway.migrate();

        log.info("Successfully migrated schema: {}", schemaName);
    }
}

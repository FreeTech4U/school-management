package com.schoolsaas.dashboard.service;

import com.schoolsaas.common.util.SchemaNameValidator;
import com.schoolsaas.config.multitenancy.TenantContext;
import com.schoolsaas.dashboard.dto.response.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * CORRECTION 1 : clé de cache par tenant.
     * Avec key = "'global'", toutes les écoles partageaient la même entrée
     * de cache — l'école B voyait les stats de l'école A.
     */
    @Cacheable(
            value = "dashboard_stats",
            key = "T(com.schoolsaas.config.multitenancy.TenantContext).get()"
    )
    public DashboardStatsResponse getStats() {
        String schema = currentSchema();

        // CORRECTION 2 : qualifier le schema.
        // JdbcTemplate ne passe pas par le MultiTenantConnectionProvider
        // d'Hibernate, donc TenantContext n'affecte pas le search_path ici.
        String sql = "SELECT * FROM " + schema + ".mv_dashboard_stats";

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                DashboardStatsResponse.builder()
                        .activeStudents(rs.getLong("active_students"))
                        .totalStudents(rs.getLong("total_students"))
                        .maleStudents(rs.getLong("male_students"))
                        .femaleStudents(rs.getLong("female_students"))
                        .totalFeesExpected(rs.getBigDecimal("total_fees_expected"))
                        .totalFeesCollected(rs.getBigDecimal("total_fees_collected"))
                        .collectionRatePct(rs.getDouble("collection_rate_pct"))
                        .studentsWithDebt(rs.getLong("students_with_debt"))
                        .studentsOverdue(rs.getLong("students_overdue"))
                        .pendingGradeEntries(rs.getLong("pending_grade_entries"))
                        .smsToday(rs.getLong("sms_today"))
                        .smsThisMonth(rs.getLong("sms_this_month"))
                        .build()
        );
    }

    public void refreshStats() {
        String schema = currentSchema();
        String view   = schema + ".mv_dashboard_stats";

        try {
            // CONCURRENTLY : ne verrouille pas la vue pendant le recalcul
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + view);

        } catch (Exception e) {
            // CONCURRENTLY échoue tant que la vue n'a jamais été peuplée
            // (elle est créée avec WITH NO DATA dans la migration Flyway).
            // Premier appel : refresh classique pour l'initialiser.
            log.debug("Premier peuplement de {} — refresh non-concurrent", view);
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW " + view);
        }
    }

    /**
     * Récupère et valide le schema du tenant courant.
     *
     * La validation regex est indispensable : PostgreSQL n'autorise pas
     * les paramètres préparés (?) pour les noms d'objets (schema, table).
     * On est obligé de concaténer, donc on garde-fou contre l'injection SQL.
     */
    private String currentSchema() {
        String schema = TenantContext.get();

        if (!SchemaNameValidator.isValid(schema)) {
            throw new IllegalStateException(
                    "Aucun tenant valide dans le contexte courant: " + schema);
        }
        return schema;
    }
}

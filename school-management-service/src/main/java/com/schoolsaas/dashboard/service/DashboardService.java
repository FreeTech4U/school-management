package com.schoolsaas.dashboard.service;

import com.schoolsaas.dashboard.dto.response.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    @Cacheable(value = "dashboard_stats", key = "'global'")
    public DashboardStatsResponse getStats() {
        String sql = "SELECT * FROM mv_dashboard_stats";
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> DashboardStatsResponse.builder()
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
                .build());
    }

    public void refreshStats() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_stats");
    }
}

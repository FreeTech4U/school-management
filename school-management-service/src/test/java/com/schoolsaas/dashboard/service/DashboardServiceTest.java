package com.schoolsaas.dashboard.service;

import com.schoolsaas.config.multitenancy.TenantContext;
import com.schoolsaas.dashboard.dto.response.DashboardStatsResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // getStats()/refreshStats() qualifient leurs requêtes avec le schema
        // du tenant courant (JdbcTemplate ne passe pas par le
        // MultiTenantConnectionProvider) — il faut donc le positionner ici,
        // comme le fait JwtAuthenticationFilter à l'exécution.
        TenantContext.set("test_school");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getStats_Success() {
        // Given
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .activeStudents(100L)
                .totalFeesExpected(BigDecimal.valueOf(10000))
                .build();

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class))).thenReturn(stats);

        // When
        DashboardStatsResponse result = dashboardService.getStats();

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getActiveStudents());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class));
    }

    @Test
    void refreshStats_Success() {
        // When
        dashboardService.refreshStats();

        // Then
        verify(jdbcTemplate).execute(anyString());
    }
}

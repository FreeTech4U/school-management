package com.schoolsaas.dashboard.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.dashboard.dto.response.DashboardStatsResponse;
import com.schoolsaas.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/school/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT', 'TEACHER')")
    public ApiResponse<DashboardStatsResponse> getStats() {
        return ApiResponse.ok(dashboardService.getStats());
    }
}

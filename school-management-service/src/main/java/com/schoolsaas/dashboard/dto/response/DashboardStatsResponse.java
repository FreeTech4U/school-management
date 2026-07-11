package com.schoolsaas.dashboard.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalStudents;
    private long activeStudents;
    private long maleStudents;
    private long femaleStudents;
    private BigDecimal totalFeesExpected;
    private BigDecimal totalFeesCollected;
    private Double collectionRatePct;
    private long studentsWithDebt;
    private long studentsOverdue;
    private long pendingGradeEntries;
    private long smsToday;
    private long smsThisMonth;
}

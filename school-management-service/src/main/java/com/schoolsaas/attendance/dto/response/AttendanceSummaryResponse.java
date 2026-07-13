package com.schoolsaas.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    private UUID studentId;
    private Integer totalDays;
    private Integer presentDays;
    private Integer absentDays;
    private Integer lateDays;
    private Integer excusedDays;
    private Double attendanceRate;
}

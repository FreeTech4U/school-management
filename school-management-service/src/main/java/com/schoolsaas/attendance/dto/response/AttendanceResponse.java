package com.schoolsaas.attendance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AttendanceResponse {
    private UUID id;
    private UUID enrollmentId;
    private LocalDate date;
    private String period;
    private String status;
    private String justification;
}

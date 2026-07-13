package com.schoolsaas.attendance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAttendanceRequest {
    
    @NotNull(message = "classId is required")
    private UUID classId;
    
    @NotNull(message = "date is required")
    private LocalDate date;
    
    @NotBlank(message = "period is required")
    private String period; // FULL_DAY|MORNING|AFTERNOON
    
    @Valid
    @NotEmpty(message = "records cannot be empty")
    private List<AttendanceRecord> records;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceRecord {
        @NotNull(message = "enrollmentId is required")
        private UUID enrollmentId;
        
        @NotBlank(message = "status is required")
        private String status; // PRESENT|ABSENT|LATE|EXCUSED
        
        @Size(max = 200)
        private String justification;
    }
}

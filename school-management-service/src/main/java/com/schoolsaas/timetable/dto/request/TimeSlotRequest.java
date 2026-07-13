package com.schoolsaas.timetable.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotRequest {
    
    @NotBlank(message = "dayOfWeek is required")
    private String dayOfWeek; // MONDAY..SATURDAY
    
    @NotNull(message = "startTime is required")
    private LocalTime startTime;
    
    @NotNull(message = "endTime is required")
    private LocalTime endTime;
    
    @NotBlank(message = "label is required")
    @Size(min = 1, max = 50)
    private String label;
    
    @Min(value = 0, message = "orderIndex must be >= 0")
    private Integer orderIndex;
}

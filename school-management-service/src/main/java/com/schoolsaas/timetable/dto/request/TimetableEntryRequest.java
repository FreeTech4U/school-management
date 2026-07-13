package com.schoolsaas.timetable.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntryRequest {
    
    @NotNull(message = "classSubjectId is required")
    private UUID classSubjectId;
    
    @NotNull(message = "timeSlotId is required")
    private UUID timeSlotId;
    
    @NotNull(message = "academicYearId is required")
    private UUID academicYearId;
    
    private UUID termId; // optional: null = toute l'année
    
    @NotBlank(message = "roomNumber is required")
    private String roomNumber;
    
    private Boolean isActive = true;
}

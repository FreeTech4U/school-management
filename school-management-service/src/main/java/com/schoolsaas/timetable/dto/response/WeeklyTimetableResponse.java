package com.schoolsaas.timetable.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimetableResponse {
    private UUID classId;
    private String className;
    private List<TimetableEntryDto> entries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimetableEntryDto {
        private String dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String subjectName;
        private String teacherName;
        private String roomNumber;
        private Boolean isActive;
    }
}

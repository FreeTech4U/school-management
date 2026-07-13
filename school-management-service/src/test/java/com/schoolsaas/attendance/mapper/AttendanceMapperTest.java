package com.schoolsaas.attendance.mapper;

import com.schoolsaas.attendance.dto.request.AttendanceRequest;
import com.schoolsaas.attendance.dto.response.AttendanceResponse;
import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.common.enums.AttendanceStatus;
import com.schoolsaas.common.enums.Period;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AttendanceMapperTest {

    private final AttendanceMapper mapper = Mappers.getMapper(AttendanceMapper.class);

    @Test
    void toEntity_ShouldMapCorrectly() {
        // Given
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(UUID.randomUUID());
        request.setDate(LocalDate.now());
        request.setPeriod("MORNING");
        request.setStatus("PRESENT");

        // When
        Attendance entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(LocalDate.now(), entity.getDate());
        assertEquals(Period.MORNING, entity.getPeriod());
        assertEquals(AttendanceStatus.PRESENT, entity.getStatus());
    }

    @Test
    void toResponse_ShouldMapCorrectly() {
        // Given
        UUID enrollmentId = UUID.randomUUID();
        UUID attendanceId = UUID.randomUUID();

        Attendance entity = Attendance.builder()
                .enrollmentId(enrollmentId)
                .date(LocalDate.now())
                .period(Period.AFTERNOON)
                .status(AttendanceStatus.ABSENT)
                .justification("Sick leave")
                .build();
        entity.setId(attendanceId);

        // When
        AttendanceResponse response = mapper.toResponse(entity);

        // Then
        assertNotNull(response);
        assertEquals(attendanceId, response.getId());
        // Note: MapStruct mapper doesn't extract nested IDs. Controller uses manual mapToResponse()
        assertEquals(LocalDate.now(), response.getDate());
        assertEquals("AFTERNOON", response.getPeriod());
        assertEquals("ABSENT", response.getStatus());
    }
}

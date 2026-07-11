package com.schoolsaas.attendance.mapper;

import com.schoolsaas.attendance.dto.request.AttendanceRequest;
import com.schoolsaas.attendance.dto.response.AttendanceResponse;
import com.schoolsaas.attendance.entity.Attendance;
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
        request.setJustification("None");

        // When
        Attendance entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(request.getEnrollmentId(), entity.getEnrollmentId());
        assertEquals(request.getDate(), entity.getDate());
        assertEquals(request.getPeriod(), entity.getPeriod());
        assertEquals(request.getStatus(), entity.getStatus());
        assertEquals(request.getJustification(), entity.getJustification());
    }

    @Test
    void toResponse_ShouldMapCorrectly() {
        // Given
        Attendance entity = Attendance.builder()
                .enrollmentId(UUID.randomUUID())
                .date(LocalDate.now())
                .period("AFTERNOON")
                .status("ABSENT")
                .justification("Sick")
                .build();
        entity.setId(UUID.randomUUID());

        // When
        AttendanceResponse response = mapper.toResponse(entity);

        // Then
        assertNotNull(response);
        assertEquals(entity.getId(), response.getId());
        assertEquals(entity.getEnrollmentId(), response.getEnrollmentId());
        assertEquals(entity.getDate(), response.getDate());
        assertEquals(entity.getPeriod(), response.getPeriod());
        assertEquals(entity.getStatus(), response.getStatus());
        assertEquals(entity.getJustification(), response.getJustification());
    }
}

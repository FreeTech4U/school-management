package com.schoolsaas.attendance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.attendance.dto.request.AttendanceRequest;
import com.schoolsaas.attendance.dto.response.AttendanceResponse;
import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.attendance.mapper.AttendanceMapper;
import com.schoolsaas.attendance.service.AttendanceService;
import com.schoolsaas.config.security.JwtAuthenticationEntryPoint;
import com.schoolsaas.config.security.JwtAuthenticationFilter;
import com.schoolsaas.config.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AttendanceService attendanceService;

    @MockBean
    private AttendanceMapper attendanceMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void recordAttendance_ShouldReturnSavedAttendance() throws Exception {
        // Given
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(UUID.randomUUID());
        request.setDate(LocalDate.now());
        request.setPeriod("MORNING");
        request.setStatus("PRESENT");

        Attendance attendance = new Attendance();
        AttendanceResponse response = AttendanceResponse.builder()
                .id(UUID.randomUUID())
                .status("PRESENT")
                .build();

        when(attendanceMapper.toEntity(any(AttendanceRequest.class))).thenReturn(attendance);
        when(attendanceService.recordAttendance(any(Attendance.class))).thenReturn(attendance);
        when(attendanceMapper.toResponse(any(Attendance.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/school/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));
    }
}

package com.schoolsaas.attendance.controller;

import com.schoolsaas.attendance.dto.request.AttendanceRequest;
import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.attendance.repository.AttendanceRepository;
import com.schoolsaas.attendance.service.AttendanceService;
import com.schoolsaas.common.enums.AttendanceStatus;
import com.schoolsaas.common.enums.Period;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
class AttendanceControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceService attendanceService;
    @MockBean
    private AttendanceRepository attendanceRepository;
    @MockBean
    private StudentEnrollmentRepository enrollmentRepository;

    @Test
    @WithMockUser(roles = "TEACHER")
    void recordAttendance_WithTeacherRole_ShouldCreateAttendance() throws Exception {
        AttendanceRequest request = validAttendanceRequest();
        StudentEnrollment enrollment = enrollment(request.getEnrollmentId());
        Attendance attendance = attendance(request.getEnrollmentId(), UUID.randomUUID());

        when(enrollmentRepository.findById(request.getEnrollmentId())).thenReturn(Optional.of(enrollment));
        when(attendanceService.recordAttendance(any(Attendance.class))).thenReturn(attendance);

        mockMvc.perform(post("/api/v1/school/attendance")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enrollmentId").value(request.getEnrollmentId().toString()))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));
    }

    @Test
    void recordAttendance_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/school/attendance")
                        .contentType("application/json")
                        .content(asJson(validAttendanceRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void recordAttendance_WithUnauthorizedRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/school/attendance")
                        .contentType("application/json")
                        .content(asJson(validAttendanceRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void queryAttendance_WithTeacherRole_ShouldReturnAttendanceList() throws Exception {
        UUID enrollmentId = UUID.randomUUID();
        Attendance attendance = attendance(enrollmentId, UUID.randomUUID());
        when(attendanceRepository.findByEnrollmentIdAndDate(enrollmentId, LocalDate.now())).thenReturn(List.of(attendance));

        mockMvc.perform(get("/api/v1/school/attendance")
                        .param("enrollmentId", enrollmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].period").value("MORNING"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void getStudentAttendanceSummary_WithDirectorRole_ShouldReturnComputedSummary() throws Exception {
        UUID studentId = UUID.randomUUID();
        when(attendanceRepository.findAll()).thenReturn(List.of(
                attendance(UUID.randomUUID(), studentId, AttendanceStatus.PRESENT),
                attendance(UUID.randomUUID(), studentId, AttendanceStatus.ABSENT),
                attendance(UUID.randomUUID(), studentId, AttendanceStatus.PRESENT)
        ));

        mockMvc.perform(get("/api/v1/school/attendance/student/{studentId}/summary", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.data.totalDays").value(3))
                .andExpect(jsonPath("$.data.presentDays").value(2))
                .andExpect(jsonPath("$.data.absentDays").value(1))
                .andExpect(jsonPath("$.data.attendanceRate").value(66.66666666666666));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateAttendance_WithTeacherRole_ShouldUpdateAttendance() throws Exception {
        AttendanceRequest request = validAttendanceRequest();
        Attendance attendance = attendance(request.getEnrollmentId(), UUID.randomUUID());
        when(attendanceRepository.findById(attendance.getId())).thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/school/attendance/{id}", attendance.getId())
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.justification").value("Présent"))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));
    }

    private AttendanceRequest validAttendanceRequest() {
        AttendanceRequest request = new AttendanceRequest();
        request.setEnrollmentId(UUID.randomUUID());
        request.setDate(LocalDate.of(2026, 2, 17));
        request.setPeriod("MORNING");
        request.setStatus("PRESENT");
        request.setJustification("Présent");
        return request;
    }

    private StudentEnrollment enrollment(UUID id) {
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(UUID.randomUUID())
                .classId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .build();
        enrollment.setId(id);
        return enrollment;
    }

    private Attendance attendance(UUID enrollmentId, UUID studentId) {
        return attendance(enrollmentId, studentId, AttendanceStatus.PRESENT);
    }

    private Attendance attendance(UUID enrollmentId, UUID studentId, AttendanceStatus status) {
        StudentEnrollment enrollment = enrollment(enrollmentId);
        enrollment.setStudentId(studentId);

        Attendance attendance = Attendance.builder()
                .enrollment(enrollment)
                .date(LocalDate.of(2026, 2, 17))
                .period(Period.MORNING)
                .status(status)
                .justification("Présent")
                .build();
        attendance.setId(UUID.randomUUID());
        return attendance;
    }
}

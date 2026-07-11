package com.schoolsaas.attendance.service;

import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.attendance.repository.AttendanceRepository;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.communication.service.SmsService;
import com.schoolsaas.enrollment.entity.Student;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.enrollment.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentEnrollmentRepository enrollmentRepository;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void recordAttendance_Present_Success() {
        // Given
        Attendance attendance = new Attendance();
        attendance.setEnrollmentId(UUID.randomUUID());
        attendance.setDate(LocalDate.now());
        attendance.setPeriod("MORNING");
        attendance.setStatus("PRESENT");

        when(attendanceRepository.existsByEnrollmentIdAndDateAndPeriod(any(), any(), any())).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        // When
        Attendance result = attendanceService.recordAttendance(attendance);

        // Then
        assertNotNull(result);
        verify(attendanceRepository).save(attendance);
        verifyNoInteractions(smsService);
    }

    @Test
    void recordAttendance_Absent_SendsSms() {
        // Given
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Attendance attendance = new Attendance();
        attendance.setEnrollmentId(enrollmentId);
        attendance.setDate(LocalDate.now());
        attendance.setPeriod("MORNING");
        attendance.setStatus("ABSENT");

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setId(enrollmentId);
        enrollment.setStudentId(studentId);

        Student student = new Student();
        student.setId(studentId);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setParentPhone("+224622112233");

        when(attendanceRepository.existsByEnrollmentIdAndDateAndPeriod(any(), any(), any())).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        // When
        attendanceService.recordAttendance(attendance);

        // Then
        verify(smsService).sendTemplatedSms(eq(studentId), eq("+224622112233"), eq("absence_notification"), anyMap());
    }

    @Test
    void recordAttendance_Duplicate_ThrowsException() {
        // Given
        Attendance attendance = new Attendance();
        attendance.setEnrollmentId(UUID.randomUUID());
        attendance.setDate(LocalDate.now());
        attendance.setPeriod("MORNING");

        when(attendanceRepository.existsByEnrollmentIdAndDateAndPeriod(any(), any(), any())).thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> attendanceService.recordAttendance(attendance));
    }
}

package com.schoolsaas.attendance.service;

import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.attendance.repository.AttendanceRepository;
import com.schoolsaas.common.enums.AttendanceStatus;
import com.schoolsaas.common.enums.Period;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.entity.Student;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentRepository;
import com.schoolsaas.communication.service.SmsService;
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
    private SmsService smsService;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void recordAttendance_Success() {
        // Given
        UUID enrollmentId = UUID.randomUUID();
        StudentEnrollment enrollment = StudentEnrollment.builder().build();
        enrollment.setId(enrollmentId);

        Attendance attendance = Attendance.builder()
                .enrollment(enrollment)
                .date(LocalDate.now())
                .period(Period.MORNING)
                .status(AttendanceStatus.PRESENT)
                .build();

        when(attendanceRepository.existsByEnrollmentIdAndDateAndPeriod(enrollmentId, LocalDate.now(), Period.MORNING))
                .thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        // When
        Attendance result = attendanceService.recordAttendance(attendance);

        // Then
        assertNotNull(result);
        assertEquals(AttendanceStatus.PRESENT, result.getStatus());
        verify(attendanceRepository).save(attendance);
        verify(smsService, never()).sendTemplatedSms(any(), any(), any(), any());
    }

    @Test
    void recordAttendance_WithAbsent_ShouldNotifyParent() {
        // Given
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        
        StudentEnrollment enrollment = StudentEnrollment.builder().build();
        enrollment.setId(enrollmentId);
        enrollment.setStudentId(studentId);

        Student student = Student.builder()
                .parentPhone("+1234567890")
                .parentName("Parent Name")
                .firstName("John")
                .lastName("Doe")
                .build();
        student.setId(studentId);

        Attendance attendance = Attendance.builder()
                .enrollment(enrollment)
                .date(LocalDate.now())
                .period(Period.AFTERNOON)
                .status(AttendanceStatus.ABSENT)
                .build();

        when(attendanceRepository.existsByEnrollmentIdAndDateAndPeriod(enrollmentId, LocalDate.now(), Period.AFTERNOON))
                .thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        // When
        Attendance result = attendanceService.recordAttendance(attendance);

        // Then
        assertNotNull(result);
        assertEquals(AttendanceStatus.ABSENT, result.getStatus());
        verify(smsService).sendTemplatedSms(eq(studentId), eq("+1234567890"), eq("absence_notification"), any());
    }

    @Test
    void recordAttendance_AlreadyRecorded_ThrowsException() {
        // Given
        UUID enrollmentId = UUID.randomUUID();
        StudentEnrollment enrollment = StudentEnrollment.builder().build();
        enrollment.setId(enrollmentId);

        Attendance attendance = Attendance.builder()
                .enrollment(enrollment)
                .date(LocalDate.now())
                .period(Period.FULL_DAY)
                .status(AttendanceStatus.PRESENT)
                .build();

        when(attendanceRepository.existsByEnrollmentIdAndDateAndPeriod(enrollmentId, LocalDate.now(), Period.FULL_DAY))
                .thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> attendanceService.recordAttendance(attendance));
        verify(attendanceRepository, never()).save(any());
    }
}

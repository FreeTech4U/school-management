package com.schoolsaas.enrollment.service;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.entity.SchoolClass;
import com.schoolsaas.academic.repository.AcademicYearRepository;
import com.schoolsaas.academic.repository.SchoolClassRepository;
import com.schoolsaas.common.enums.EnrollmentStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.dto.request.EnrollStudentRequest;
import com.schoolsaas.enrollment.dto.response.EnrollmentResponse;
import com.schoolsaas.enrollment.entity.Student;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.enrollment.repository.StudentRepository;
import com.schoolsaas.finance.service.StudentFeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SchoolClassRepository classRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private StudentFeeService studentFeeService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void enroll_Success() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();

        EnrollStudentRequest request = new EnrollStudentRequest();
        request.setStudentId(studentId);
        request.setClassId(classId);
        request.setAcademicYearId(yearId);
        request.setEnrollmentDate(LocalDate.now());
        request.setIsRepeating(false);

        Student student = new Student();
        student.setId(studentId);
        student.setFirstName("John");
        student.setLastName("Doe");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("10th Grade");

        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setLabel("2023-2024");

        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId)).thenReturn(Optional.empty());
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));
        when(enrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(i -> i.getArgument(0));

        // When
        EnrollmentResponse response = enrollmentService.enroll(request);

        // Then
        assertNotNull(response);
        assertEquals("ACTIVE", response.getStatus());
        verify(studentFeeService).generateFeesForEnrollment(any(StudentEnrollment.class));
        verify(enrollmentRepository).save(any(StudentEnrollment.class));
    }

    @Test
    void enroll_AlreadyEnrolled_ThrowsException() {
        // Given
        UUID studentId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        EnrollStudentRequest request = new EnrollStudentRequest();
        request.setStudentId(studentId);
        request.setAcademicYearId(yearId);

        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(new StudentEnrollment()));

        // When & Then
        assertThrows(BusinessException.class, () -> enrollmentService.enroll(request));
    }

    @Test
    void withdrawStudent_Success() {
        // Given
        UUID enrollmentId = UUID.randomUUID();
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setId(enrollmentId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        // When
        enrollmentService.withdrawStudent(enrollmentId);

        // Then
        assertEquals(EnrollmentStatus.DROPPED_OUT, enrollment.getStatus());
        verify(enrollmentRepository).save(enrollment);
    }
}

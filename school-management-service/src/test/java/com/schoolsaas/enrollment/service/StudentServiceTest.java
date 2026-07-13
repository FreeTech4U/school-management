package com.schoolsaas.enrollment.service;

import com.schoolsaas.common.enums.Gender;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.dto.request.CreateStudentRequest;
import com.schoolsaas.enrollment.dto.response.StudentResponse;
import com.schoolsaas.enrollment.entity.Student;
import com.schoolsaas.enrollment.mapper.EnrollmentMapper;
import com.schoolsaas.enrollment.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EnrollmentMapper enrollmentMapper;

    @InjectMocks
    private StudentService studentService;

    @Test
    void getAllStudents_WithSearch_UsesSearchRepositoryMethod() {
        Student student = student();
        StudentResponse response = response(student);
        PageRequest pageable = PageRequest.of(0, 10);

        when(studentRepository.searchStudents("diallo", pageable)).thenReturn(new PageImpl<>(List.of(student), pageable, 1));
        when(enrollmentMapper.toStudentResponse(student)).thenReturn(response);

        Page<StudentResponse> result = studentService.getAllStudents("diallo", pageable);

        assertEquals(1, result.getTotalElements());
        verify(studentRepository).searchStudents("diallo", pageable);
        verify(studentRepository, never()).findAll(pageable);
    }

    @Test
    void getStudentById_WhenMissing_ThrowsBusinessException() {
        UUID id = UUID.randomUUID();
        when(studentRepository.findById(id)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> studentService.getStudentById(id));

        assertEquals("STUDENT_NOT_FOUND", ex.getCode());
    }

    @Test
    void createStudent_WithValidGuineanPhone_NormalizesPhone() {
        CreateStudentRequest request = studentRequest();
        Student saved = student();
        saved.setParentPhone("+224622334455");
        StudentResponse response = response(saved);

        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentMapper.toStudentResponse(any(Student.class))).thenReturn(response);

        StudentResponse result = studentService.createStudent(request);

        assertEquals("+224622334455", result.getParentPhone());
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void createStudent_WithInvalidPhone_ThrowsBusinessException() {
        CreateStudentRequest request = studentRequest();
        request.setParentPhone("12345");

        BusinessException ex = assertThrows(BusinessException.class, () -> studentService.createStudent(request));

        assertEquals("INVALID_PHONE", ex.getCode());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void updateStudent_WithValidPhone_UpdatesAndNormalizesPhone() {
        UUID id = UUID.randomUUID();
        Student existing = student();
        existing.setId(id);
        CreateStudentRequest request = studentRequest();
        request.setParentPhone("0622334455");

        when(studentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(studentRepository.save(existing)).thenReturn(existing);
        when(enrollmentMapper.toStudentResponse(existing)).thenAnswer(invocation -> response(invocation.getArgument(0)));

        StudentResponse result = studentService.updateStudent(id, request);

        assertEquals("+224622334455", existing.getParentPhone());
        assertEquals("Awa", result.getFirstName());
        verify(studentRepository).save(existing);
    }

    @Test
    void deleteStudent_SetsInactiveFlag() {
        UUID id = UUID.randomUUID();
        Student existing = student();
        existing.setId(id);
        existing.setIsActive(true);

        when(studentRepository.findById(id)).thenReturn(Optional.of(existing));

        studentService.deleteStudent(id);

        assertFalse(existing.getIsActive());
        verify(studentRepository).save(existing);
    }

    private CreateStudentRequest studentRequest() {
        CreateStudentRequest request = new CreateStudentRequest();
        request.setFirstName("Awa");
        request.setLastName("Diallo");
        request.setDateOfBirth(LocalDate.of(2012, 5, 10));
        request.setGender(Gender.MALE);
        request.setBirthCity("Conakry");
        request.setBirthCountry("GN");
        request.setAddress("Matoto");
        request.setParentName("Mamadou Diallo");
        request.setParentPhone("622334455");
        request.setMedicalNotes("None");
        return request;
    }

    private Student student() {
        return Student.builder()
                .firstName("Awa")
                .lastName("Diallo")
                .dateOfBirth(LocalDate.of(2012, 5, 10))
                .gender(Gender.FEMALE)
                .birthCity("Conakry")
                .birthCountry("GN")
                .address("Matoto")
                .parentName("Mamadou Diallo")
                .parentPhone("+224622334455")
                .medicalNotes("None")
                .isActive(true)
                .build();
    }

    private StudentResponse response(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .studentNumber(student.getStudentNumber())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .parentName(student.getParentName())
                .parentPhone(student.getParentPhone())
                .isActive(student.getIsActive())
                .build();
    }
}

package com.schoolsaas.enrollment.service;

import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.common.util.PhoneUtils;
import com.schoolsaas.enrollment.dto.request.CreateStudentRequest;
import com.schoolsaas.enrollment.dto.response.StudentResponse;
import com.schoolsaas.enrollment.entity.Student;
import com.schoolsaas.enrollment.mapper.EnrollmentMapper;
import com.schoolsaas.enrollment.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentMapper enrollmentMapper;

    public Page<StudentResponse> getAllStudents(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return studentRepository.searchStudents(search, pageable).map(enrollmentMapper::toStudentResponse);
        }
        return studentRepository.findAll(pageable).map(enrollmentMapper::toStudentResponse);
    }

    public StudentResponse getStudentById(UUID id) {
        return studentRepository.findById(id)
                .map(enrollmentMapper::toStudentResponse)
                .orElseThrow(() -> BusinessException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
    }

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        if (request.getParentPhone() != null && !request.getParentPhone().isBlank()) {
            if (!PhoneUtils.isValidGuineanPhoneNumber(request.getParentPhone())) {
                throw new BusinessException("INVALID_PHONE", "Format de numéro de téléphone parent invalide (+224...)");
            }
        }

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .birthCity(request.getBirthCity())
                .birthCountry(request.getBirthCountry())
                .address(request.getAddress())
                .parentName(request.getParentName())
                .parentPhone(PhoneUtils.normalizeGuineanPhoneNumber(request.getParentPhone()))
                .medicalNotes(request.getMedicalNotes())
                .isActive(true)
                .build();

        return enrollmentMapper.toStudentResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse updateStudent(UUID id, CreateStudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));

        if (request.getParentPhone() != null && !request.getParentPhone().isBlank()) {
            if (!PhoneUtils.isValidGuineanPhoneNumber(request.getParentPhone())) {
                throw new BusinessException("INVALID_PHONE", "Format de numéro de téléphone parent invalide (+224...)");
            }
        }

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setBirthCity(request.getBirthCity());
        student.setBirthCountry(request.getBirthCountry());
        student.setAddress(request.getAddress());
        student.setParentName(request.getParentName());
        student.setParentPhone(PhoneUtils.normalizeGuineanPhoneNumber(request.getParentPhone()));
        student.setMedicalNotes(request.getMedicalNotes());

        return enrollmentMapper.toStudentResponse(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
        student.setIsActive(false);
        studentRepository.save(student);
    }
}

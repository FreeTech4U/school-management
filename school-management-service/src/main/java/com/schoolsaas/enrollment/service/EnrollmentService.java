package com.schoolsaas.enrollment.service;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.entity.SchoolClass;
import com.schoolsaas.academic.repository.AcademicYearRepository;
import com.schoolsaas.academic.repository.SchoolClassRepository;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.dto.request.EnrollStudentRequest;
import com.schoolsaas.enrollment.dto.response.EnrollmentResponse;
import com.schoolsaas.enrollment.entity.Student;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.enrollment.repository.StudentRepository;
import com.schoolsaas.finance.service.StudentFeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentFeeService studentFeeService;

    @Transactional
    public EnrollmentResponse enroll(EnrollStudentRequest request) {
        // 1. Check if already enrolled in this year
        enrollmentRepository.findByStudentIdAndAcademicYearId(request.getStudentId(), request.getAcademicYearId())
                .ifPresent(e -> {
                    throw BusinessException.conflict("ALREADY_ENROLLED", "Cet élève est déjà inscrit pour cette année scolaire");
                });

        // 2. Validate entities exist
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> BusinessException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
        
        SchoolClass schoolClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> BusinessException.notFound("CLASS_NOT_FOUND", "Classe introuvable"));
        
        AcademicYear year = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> BusinessException.notFound("ACADEMIC_YEAR_NOT_FOUND", "Année scolaire introuvable"));

        // 3. Create Enrollment
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(student.getId())
                .classId(schoolClass.getId())
                .academicYearId(year.getId())
                .enrollmentDate(request.getEnrollmentDate())
                .isRepeating(request.getIsRepeating())
                .status("ENROLLED")
                .promotionStatus("PENDING")
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        // 4. Generate fees
        studentFeeService.generateFeesForEnrollment(enrollment);

        return mapToResponse(enrollment, student, schoolClass, year);
    }

    public List<EnrollmentResponse> getEnrollmentsByClass(UUID classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> BusinessException.notFound("CLASS_NOT_FOUND", "Classe introuvable"));
        
        return enrollmentRepository.findByClassId(classId).stream()
                .map(e -> {
                    Student s = studentRepository.findById(e.getStudentId()).orElse(null);
                    AcademicYear y = academicYearRepository.findById(e.getAcademicYearId()).orElse(null);
                    return mapToResponse(e, s, schoolClass, y);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void transferStudent(UUID enrollmentId, String reason) {
        StudentEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> BusinessException.notFound("ENROLLMENT_NOT_FOUND", "Inscription introuvable"));
        enrollment.setStatus("TRANSFERRED");
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void withdrawStudent(UUID enrollmentId) {
        StudentEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> BusinessException.notFound("ENROLLMENT_NOT_FOUND", "Inscription introuvable"));
        enrollment.setStatus("WITHDRAWN");
        enrollmentRepository.save(enrollment);
    }

    private EnrollmentResponse mapToResponse(StudentEnrollment e, Student s, SchoolClass c, AcademicYear y) {
        return EnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .studentName(s != null ? s.getFirstName() + " " + s.getLastName() : "Unknown")
                .studentNumber(s != null ? s.getStudentNumber() : null)
                .classId(e.getClassId())
                .className(c != null ? c.getName() : "Unknown")
                .academicYearId(e.getAcademicYearId())
                .academicYearLabel(y != null ? y.getLabel() : "Unknown")
                .enrollmentDate(e.getEnrollmentDate())
                .isRepeating(e.getIsRepeating())
                .status(e.getStatus())
                .promotionStatus(e.getPromotionStatus())
                .finalAverage(e.getFinalAverage())
                .build();
    }
}

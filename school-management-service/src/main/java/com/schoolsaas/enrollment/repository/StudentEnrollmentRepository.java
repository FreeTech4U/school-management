package com.schoolsaas.enrollment.repository;

import com.schoolsaas.enrollment.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    Optional<StudentEnrollment> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);
    List<StudentEnrollment> findByClassId(UUID classId);
    List<StudentEnrollment> findByStudentId(UUID studentId);
}

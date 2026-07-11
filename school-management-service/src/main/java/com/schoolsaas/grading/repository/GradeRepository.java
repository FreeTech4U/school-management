package com.schoolsaas.grading.repository;

import com.schoolsaas.grading.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {
    List<Grade> findByEnrollmentIdAndTermId(UUID enrollmentId, UUID termId);
    List<Grade> findByClassSubjectIdAndTermId(UUID classSubjectId, UUID termId);
}

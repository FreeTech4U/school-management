package com.schoolsaas.academic.repository;

import com.schoolsaas.academic.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {
    List<SchoolClass> findByAcademicYearId(UUID academicYearId);
}

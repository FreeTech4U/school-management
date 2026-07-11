package com.schoolsaas.finance.repository;

import com.schoolsaas.finance.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, UUID> {
    List<FeeStructure> findByAcademicYearId(UUID academicYearId);
}

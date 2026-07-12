package com.schoolsaas.enrollment.repository;

import com.schoolsaas.enrollment.entity.PromotionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionBatchRepository extends JpaRepository<PromotionBatch, UUID> {
    List<PromotionBatch> findByAcademicYearIdAndStatus(UUID academicYearId, String status);
    List<PromotionBatch> findByClassIdAndStatus(UUID classId, String status);
    Optional<PromotionBatch> findByIdAndStatus(UUID id, String status);
}

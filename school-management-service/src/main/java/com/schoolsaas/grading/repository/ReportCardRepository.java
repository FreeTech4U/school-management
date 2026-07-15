package com.schoolsaas.grading.repository;

import com.schoolsaas.grading.entity.ReportCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReportCardRepository extends JpaRepository<ReportCard, UUID> {
    Optional<ReportCard> findByEnrollmentIdAndTermId(UUID enrollmentId, UUID termId);
}

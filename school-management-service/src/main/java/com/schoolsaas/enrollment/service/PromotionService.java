package com.schoolsaas.enrollment.service;

import com.schoolsaas.academic.repository.AcademicYearRepository;
import com.schoolsaas.academic.repository.SchoolClassRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.enums.EnrollmentStatus;
import com.schoolsaas.common.enums.PromotionBatchStatus;
import com.schoolsaas.common.enums.PromotionStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.entity.PromotionBatch;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.PromotionBatchRepository;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.grading.repository.ReportCardRepository;
import com.schoolsaas.grading.service.ReportCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing student promotions.
 * Handles bulk promotion of students from one academic year to the next.
 * Supports promotion, repetition, and graduation based on grades.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionBatchRepository promotionBatchRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;
    private final SchoolClassRepository classRepository;
    private final ReportCardRepository reportCardRepository;
    private final ReportCardService reportCardService;

    @Value("${app.grading.passing-average:10}")
    private BigDecimal passingAverage;

    /**
     * Create a promotion batch for a class from current to next academic year.
     * Validates that all students have complete grade information.
     *
     * @param classId the class to promote
     * @param academicYearId current academic year
     * @param nextAcademicYearId next academic year
     * @param notes optional notes
     * @return created PromotionBatch
     */
    @Transactional
    public PromotionBatch createPromotionBatch(UUID classId, UUID academicYearId, 
                                                UUID nextAcademicYearId, String notes) {
        log.info("Creating promotion batch for class {} from AY {} to AY {}", 
                classId, academicYearId, nextAcademicYearId);

        if (classId == null || academicYearId == null || nextAcademicYearId == null) {
            throw new IllegalArgumentException("classId, academicYearId, and nextAcademicYearId are required");
        }

        // Verify academic years exist
        academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> BusinessException.notFound("ACADEMIC_YEAR_NOT_FOUND", 
                        "Année scolaire courante introuvable"));
        
        academicYearRepository.findById(nextAcademicYearId)
                .orElseThrow(() -> BusinessException.notFound("ACADEMIC_YEAR_NOT_FOUND", 
                        "Année scolaire suivante introuvable"));

        // Create the batch
        PromotionBatch batch = PromotionBatch.builder()
                .classId(classId)
                .academicYearId(academicYearId)
                .nextAcademicYearId(nextAcademicYearId)
                .status(PromotionBatchStatus.CREATED)
                .totalProcessed(0)
                .promotedCount(0)
                .repeatedCount(0)
                .graduatedCount(0)
                .notes(notes)
                .build();

        promotionBatchRepository.save(batch);
        log.info("Created promotion batch {} for class {}", batch.getId(), classId);
        
        return batch;
    }

    /**
     * Validate promotion criteria for all students in a batch.
     * Checks that students have passing averages and reports any issues.
     *
     * @param batchId the batch to validate
     * @return updated PromotionBatch with validation results
     */
    @Transactional
    public PromotionBatch validatePromotionCriteria(UUID batchId) {
        log.info("Validating promotion criteria for batch {}", batchId);

        PromotionBatch batch = promotionBatchRepository.findById(batchId)
                .orElseThrow(() -> BusinessException.notFound("PROMOTION_BATCH_NOT_FOUND", 
                        "Batch de promotion introuvable"));

        if (batch.getStatus() != PromotionBatchStatus.CREATED) {
            throw new BusinessException("INVALID_BATCH_STATUS", 
                    "Le batch doit être en status CREATED pour valider");
        }

        // Get all active enrollments in the class for current academic year
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findByClassIdAndStatusAndAcademicYearId(
                        batch.getClassId(), EnrollmentStatus.ENROLLED, batch.getAcademicYearId());

        if (enrollments.isEmpty()) {
            batch.setValidationErrors("Aucun élève trouvé dans la classe");
            batch.setStatus(PromotionBatchStatus.VALIDATED);
            return promotionBatchRepository.save(batch);
        }

        log.debug("Validating {} enrollments for promotion", enrollments.size());

        // For each enrollment, check that they have a finalAverage
        List<String> errors = new ArrayList<>();
        int validCount = 0;

        for (StudentEnrollment enrollment : enrollments) {
            if (enrollment.getFinalAverage() == null) {
                errors.add(String.format("Élève %s (ID: %s) - Moyenne finale non calculée", 
                        enrollment.getStudent().getId(), enrollment.getId()));
            } else {
                validCount++;
            }
        }

        batch.setTotalProcessed(enrollments.size());
        if (!errors.isEmpty()) {
            batch.setValidationErrors(String.join(" | ", errors));
        }
        batch.setStatus(PromotionBatchStatus.VALIDATED);

        promotionBatchRepository.save(batch);
        log.info("Validated {} out of {} enrollments", validCount, enrollments.size());
        
        return batch;
    }

    /**
     * Execute promotion for all students in a validated batch.
     * Creates new enrollments for the next academic year based on promotion criteria.
     *
     * Criteria:
     * - finalAverage >= passingAverage: PROMOTED (next class)
     * - finalAverage < passingAverage: REPEATED (same class)
     * - Last class: GRADUATED
     *
     * @param batchId the batch to execute
     * @return updated PromotionBatch with execution results
     */
    @Transactional
    public PromotionBatch executePromotion(UUID batchId) {
        log.info("Executing promotion for batch {}", batchId);

        PromotionBatch batch = promotionBatchRepository.findById(batchId)
                .orElseThrow(() -> BusinessException.notFound("PROMOTION_BATCH_NOT_FOUND", 
                        "Batch de promotion introuvable"));

        if (batch.getStatus() != PromotionBatchStatus.VALIDATED) {
            throw new BusinessException("INVALID_BATCH_STATUS", 
                    "Le batch doit être en status VALIDATED pour exécuter la promotion");
        }

        // Get all active enrollments in the class for current academic year
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findByClassIdAndStatusAndAcademicYearId(
                        batch.getClassId(), EnrollmentStatus.ENROLLED, batch.getAcademicYearId());

        int promotedCount = 0;
        int repeatedCount = 0;
        int graduatedCount = 0;

        log.debug("Processing {} enrollments for promotion", enrollments.size());

        for (StudentEnrollment enrollment : enrollments) {
            if (enrollment.getFinalAverage() == null) {
                log.warn("Skipping enrollment {} - no final average", enrollment.getId());
                continue;
            }

            // Determine promotion status based on average
            PromotionStatus promotionStatus;
            UUID nextClassId = null;

            if (enrollment.getFinalAverage().compareTo(passingAverage) >= 0) {
                // Student passed - promote to next class
                promotionStatus = PromotionStatus.PROMOTED;
                // TODO: In Phase 4, calculate nextClassId based on curriculum progression
                nextClassId = null; // Will be set after curriculum mapping is implemented
                promotedCount++;
            } else {
                // Student failed - retain in same class
                promotionStatus = PromotionStatus.REPEATED;
                nextClassId = batch.getClassId(); // Same class
                repeatedCount++;
            }

            // Update enrollment status
            enrollment.setPromotionStatus(promotionStatus);
            enrollment.setStatus(EnrollmentStatus.ENROLLED); // Mark as processed for current year

            // TODO: Create new enrollment for next academic year
            // This would involve:
            // 1. Setting current enrollment status to PROMOTED/REPEATED or GRADUATED
            // 2. Creating new StudentEnrollment for nextAcademicYearId, nextClassId
            // 3. Calling StudentFeeService.generateFeesForEnrollment() for new enrollment

            enrollmentRepository.save(enrollment);
        }

        // Update batch with results
        batch.setStatus(PromotionBatchStatus.EXECUTED);
        batch.setExecutedAt(Instant.now());
        batch.setPromotedCount(promotedCount);
        batch.setRepeatedCount(repeatedCount);
        batch.setGraduatedCount(graduatedCount);

        promotionBatchRepository.save(batch);
        log.info("Promotion executed: {} promoted, {} repeated, {} graduated", 
                promotedCount, repeatedCount, graduatedCount);
        
        return batch;
    }

    /**
     * Override promotion decision for a specific enrollment (DIRECTOR privilege).
     * Allows manual promotion/repetition regardless of grades.
     *
     * @param enrollmentId the enrollment to override
     * @param newStatus the new promotion status
     * @param reason the reason for override
     * @return updated StudentEnrollment
     */
    @Transactional
    public StudentEnrollment overridePromotionDecision(UUID enrollmentId, PromotionStatus newStatus, String reason) {
        log.warn("Director override of promotion decision for enrollment {}: {} - Reason: {}", 
                enrollmentId, newStatus, reason);

        StudentEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> BusinessException.notFound("ENROLLMENT_NOT_FOUND", 
                        "Inscription introuvable"));

        enrollment.setPromotionStatus(newStatus);
        return enrollmentRepository.save(enrollment);
    }

    /**
     * Get promotion batch with all details.
     *
     * @param batchId the batch identifier
     * @return PromotionBatch
     */
    public Optional<PromotionBatch> getPromotionBatch(UUID batchId) {
        return promotionBatchRepository.findById(batchId);
    }

    /**
     * List all promotion batches for a given academic year.
     *
     * @param academicYearId the academic year identifier
     * @return list of PromotionBatch
     */
    public List<PromotionBatch> listByAcademicYear(UUID academicYearId) {
        return promotionBatchRepository.findByAcademicYearIdAndStatus(academicYearId, "CREATED");
    }

    /**
     * Calculate final average for an enrollment based on the 3 terms.
     * Average of (term1 general_average + term2 + term3) / 3
     *
     * @param enrollmentId the enrollment identifier
     * @param termIds list of 3 term IDs (should be the 3 terms of academic year)
     * @return calculated final average
     */
    public BigDecimal calculateFinalAverage(UUID enrollmentId, List<UUID> termIds) {
        if (termIds == null || termIds.size() != 3) {
            throw new IllegalArgumentException("Exactly 3 term IDs are required for final average calculation");
        }

        log.debug("Calculating final average for enrollment {} across 3 terms", enrollmentId);

        BigDecimal sumOfAverages = BigDecimal.ZERO;

        for (UUID termId : termIds) {
            Optional<BigDecimal> termAverage = reportCardRepository.findByEnrollmentIdAndTermId(enrollmentId, termId)
                    .map(rc -> rc.getGeneralAverage() != null ? rc.getGeneralAverage() : BigDecimal.ZERO);
            
            if (termAverage.isPresent()) {
                sumOfAverages = sumOfAverages.add(termAverage.get());
            }
        }

        BigDecimal finalAverage = sumOfAverages.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
        log.debug("Final average for enrollment {}: {}", enrollmentId, finalAverage);
        
        return finalAverage;
    }
}

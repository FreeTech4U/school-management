package com.schoolsaas.grading.service;

import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.enums.EnrollmentStatus;
import com.schoolsaas.common.enums.ReportCardStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.entity.ReportCard;
import com.schoolsaas.grading.repository.GradeRepository;
import com.schoolsaas.grading.repository.ReportCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing student report cards (bulletins scolaires).
 * Handles generation, publishing, and PDF export of report cards.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCardService {

    private final ReportCardRepository reportCardRepository;
    private final GradeRepository gradeRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final TermRepository termRepository;

    /**
     * Generate report cards for all active students in a class for a specific term.
     * Calculates weighted average, class size, and creates/updates ReportCard entities.
     *
     * @param classId the class identifier
     * @param termId the term identifier
     * @return count of report cards generated
     */
    @Transactional
    public int generateForClass(UUID classId, UUID termId) {
        log.info("Generating report cards for class {} and term {}", classId, termId);

        Term term = termRepository.findById(termId)
                .orElseThrow(() -> BusinessException.notFound("TERM_NOT_FOUND", "Trimestre introuvable"));

        // 1. Load all ENROLLED students in the class for this academic year
        List<StudentEnrollment> enrollments = enrollmentRepository.findByClassIdAndStatusAndAcademicYearId(
                classId, EnrollmentStatus.ACTIVE, term.getAcademicYear().getId());

        if (enrollments.isEmpty()) {
            log.warn("No active enrollments found for class {} in term {}", classId, termId);
            return 0;
        }

        int classSize = enrollments.size();
        log.debug("Found {} active enrollments for class {}", classSize, classId);

        // 2. Generate report cards for each enrollment
        List<ReportCard> reportCards = enrollments.stream()
                .map(enrollment -> {
                    BigDecimal weightedAverage = calculateWeightedAverage(enrollment.getId(), termId);
                    
                    ReportCard existing = reportCardRepository.findByEnrollmentIdAndTermId(
                            enrollment.getId(), termId).orElse(null);
                    
                    if (existing != null) {
                        existing.setGeneralAverage(weightedAverage);
                        existing.setClassSize(classSize);
                        existing.setStatus(ReportCardStatus.DRAFT);
                        return existing;
                    } else {
                        return ReportCard.builder()
                                .enrollment(enrollment)
                                .term(term)
                                .generalAverage(weightedAverage)
                                .classSize(classSize)
                                .status(ReportCardStatus.DRAFT)
                                .build();
                    }
                })
                .collect(Collectors.toList());

        reportCardRepository.saveAll(reportCards);

        // 3. Calculate rankings (sort by average descending)
        calculateAndUpdateRankings(classId, termId);

        log.info("Generated/updated {} report cards for class {}", reportCards.size(), classId);
        return reportCards.size();
    }

    /**
     * Calculate the weighted average for a student's term.
     * Formula: Σ(subject_average × coefficient) / Σ(coefficients)
     *
     * @param enrollmentId the enrollment identifier
     * @param termId the term identifier
     * @return the calculated weighted average (0.0 if no grades)
     */
    public BigDecimal calculateWeightedAverage(UUID enrollmentId, UUID termId) {
        log.debug("Calculating weighted average for enrollment {} in term {}", enrollmentId, termId);

        // Load all grades for the enrollment in the term, eager loading classSubject for coefficient
        List<Grade> grades = gradeRepository.findByEnrollmentIdAndTermId(enrollmentId, termId);

        if (grades.isEmpty()) {
            log.debug("No grades found for enrollment {} in term {}", enrollmentId, termId);
            return BigDecimal.ZERO;
        }

        // Group grades by subject (classSubject) and calculate per-subject averages
        Map<UUID, List<Grade>> gradesBySubject = grades.stream()
                .collect(Collectors.groupingBy(g -> g.getClassSubject().getId()));

        BigDecimal totalWeightedScore = BigDecimal.ZERO;
        int totalCoefficient = 0;

        for (Map.Entry<UUID, List<Grade>> entry : gradesBySubject.entrySet()) {
            List<Grade> subjectGrades = entry.getValue();
            
            // Calculate average for this subject
            BigDecimal subjectAverage = subjectGrades.stream()
                    .map(Grade::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(subjectGrades.size()), 2, RoundingMode.HALF_UP);

            // Get coefficient (all grades in a subject have the same coefficient)
            int coefficient = subjectGrades.get(0).getClassSubject().getCoefficient();

            // Add to weighted sum
            totalWeightedScore = totalWeightedScore.add(
                    subjectAverage.multiply(BigDecimal.valueOf(coefficient))
            );
            totalCoefficient += coefficient;
        }

        if (totalCoefficient == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal weightedAverage = totalWeightedScore.divide(
                BigDecimal.valueOf(totalCoefficient), 2, RoundingMode.HALF_UP);

        log.debug("Weighted average for enrollment {}: {}", enrollmentId, weightedAverage);
        return weightedAverage;
    }

    /**
     * Publish a report card: update status, generate PDF, update published timestamp.
     * Send SMS notification to parent.
     *
     * @param reportCardId the report card identifier
     * @return updated ReportCard
     */
    @Transactional
    public ReportCard publish(UUID reportCardId) {
        log.info("Publishing report card {}", reportCardId);

        ReportCard reportCard = reportCardRepository.findById(reportCardId)
                .orElseThrow(() -> BusinessException.notFound("REPORT_CARD_NOT_FOUND", "Bulletin introuvable"));

        if (reportCard.getGeneralAverage() == null) {
            throw new BusinessException("REPORT_CARD_NOT_READY", "La moyenne générale n'a pas été calculée");
        }

        // Update status and timestamp
        reportCard.setStatus(ReportCardStatus.PUBLISHED);
        reportCard.setPublishedAt(LocalDateTime.now());

        // TODO: In Phase 4, integrate PDF generation (Thymeleaf + OpenHTMLtoPDF)
        // TODO: In Phase 4, integrate SMS notification to parent

        reportCardRepository.save(reportCard);
        log.info("Report card {} published successfully", reportCardId);
        
        return reportCard;
    }

    /**
     * Publish all report cards for a class and term.
     * Bulk operation for efficiency.
     *
     * @param classId the class identifier
     * @param termId the term identifier
     * @return count of published report cards
     */
    @Transactional
    public int publishForClass(UUID classId, UUID termId) {
        log.info("Publishing all report cards for class {} and term {}", classId, termId);

        // Get all DRAFT report cards for the class (join via enrollment)
        List<StudentEnrollment> enrollments = enrollmentRepository.findByClassId(classId);
        
        List<ReportCard> draftCards = enrollments.stream()
                .flatMap(e -> reportCardRepository.findByEnrollmentIdAndTermId(e.getId(), termId).stream())
                .filter(rc -> ReportCardStatus.DRAFT.equals(rc.getStatus()))
                .collect(Collectors.toList());

        log.debug("Found {} draft report cards to publish", draftCards.size());

        draftCards.forEach(card -> {
            card.setStatus(ReportCardStatus.PUBLISHED);
            card.setPublishedAt(LocalDateTime.now());
        });

        reportCardRepository.saveAll(draftCards);
        log.info("Published {} report cards for class {}", draftCards.size(), classId);
        
        return draftCards.size();
    }

    /**
     * Update report card comments (by director or teacher).
     *
     * @param reportCardId the report card identifier
     * @param teacherComment optional teacher comment
     * @param directorComment optional director comment
     * @return updated ReportCard
     */
    @Transactional
    public ReportCard updateComments(UUID reportCardId, String teacherComment, String directorComment) {
        log.info("Updating comments for report card {}", reportCardId);

        ReportCard reportCard = reportCardRepository.findById(reportCardId)
                .orElseThrow(() -> BusinessException.notFound("REPORT_CARD_NOT_FOUND", "Bulletin introuvable"));

        if (teacherComment != null) {
            reportCard.setTeacherComment(teacherComment);
        }
        if (directorComment != null) {
            reportCard.setDirectorComment(directorComment);
        }

        return reportCardRepository.save(reportCard);
    }

    /**
     * Retrieve report card for a specific enrollment and term.
     *
     * @param enrollmentId the enrollment identifier
     * @param termId the term identifier
     * @return ReportCard if exists
     */
    public Optional<ReportCard> getReportCard(UUID enrollmentId, UUID termId) {
        return reportCardRepository.findByEnrollmentIdAndTermId(enrollmentId, termId);
    }

    /**
     * Calculate and update rankings (rank_in_class) for all report cards in a class for a term.
     * Sorts by generalAverage descending.
     *
     * @param classId the class identifier
     * @param termId the term identifier
     */
    private void calculateAndUpdateRankings(UUID classId, UUID termId) {
        log.debug("Calculating rankings for class {} in term {}", classId, termId);

        // Get all active enrollments in the class
        List<StudentEnrollment> enrollments = enrollmentRepository.findByClassId(classId);

        // Get all report cards for these enrollments and the term
        List<ReportCard> reportCards = enrollments.stream()
                .flatMap(e -> reportCardRepository.findByEnrollmentIdAndTermId(e.getId(), termId).stream())
                .collect(Collectors.toList());

        // Sort by general average descending
        reportCards.sort((rc1, rc2) -> {
            BigDecimal avg1 = rc1.getGeneralAverage() != null ? rc1.getGeneralAverage() : BigDecimal.ZERO;
            BigDecimal avg2 = rc2.getGeneralAverage() != null ? rc2.getGeneralAverage() : BigDecimal.ZERO;
            return avg2.compareTo(avg1); // descending
        });

        // Assign rankings
        for (int i = 0; i < reportCards.size(); i++) {
            reportCards.get(i).setRankInClass(i + 1);
        }

        reportCardRepository.saveAll(reportCards);
        log.debug("Updated rankings for {} report cards", reportCards.size());
    }
}

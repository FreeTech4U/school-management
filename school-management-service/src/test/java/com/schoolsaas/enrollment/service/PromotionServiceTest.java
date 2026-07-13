package com.schoolsaas.enrollment.service;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.repository.AcademicYearRepository;
import com.schoolsaas.academic.repository.SchoolClassRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.enums.EnrollmentStatus;
import com.schoolsaas.common.enums.PromotionStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.entity.PromotionBatch;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.PromotionBatchRepository;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.grading.entity.ReportCard;
import com.schoolsaas.grading.repository.ReportCardRepository;
import com.schoolsaas.grading.service.ReportCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionBatchRepository promotionBatchRepository;
    @Mock
    private StudentEnrollmentRepository enrollmentRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private SchoolClassRepository classRepository;
    @Mock
    private ReportCardRepository reportCardRepository;
    @Mock
    private ReportCardService reportCardService;

    @InjectMocks
    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(promotionService, "passingAverage", new BigDecimal("10"));
    }

    @Test
    void createPromotionBatch_WithValidAcademicYears_CreatesCreatedBatch() {
        UUID classId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        UUID nextAcademicYearId = UUID.randomUUID();
        AcademicYear current = academicYear(academicYearId);
        AcademicYear next = academicYear(nextAcademicYearId);

        when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(current));
        when(academicYearRepository.findById(nextAcademicYearId)).thenReturn(Optional.of(next));
        when(promotionBatchRepository.save(any(PromotionBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromotionBatch batch = promotionService.createPromotionBatch(classId, academicYearId, nextAcademicYearId, "Fin d'année");

        assertEquals("CREATED", batch.getStatus());
        assertEquals(classId, batch.getClassId());
        assertEquals(0, batch.getPromotedCount());
        assertEquals("Fin d'année", batch.getNotes());
    }

    @Test
    void createPromotionBatch_WithMissingAcademicYear_ThrowsBusinessException() {
        UUID classId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        UUID nextAcademicYearId = UUID.randomUUID();

        when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> promotionService.createPromotionBatch(classId, academicYearId, nextAcademicYearId, null));

        assertEquals("ACADEMIC_YEAR_NOT_FOUND", ex.getCode());
    }

    @Test
    void validatePromotionCriteria_WithMissingFinalAverage_AddsValidationErrors() {
        UUID batchId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        PromotionBatch batch = batch(batchId, classId, academicYearId);
        StudentEnrollment valid = enrollment(UUID.randomUUID(), classId, academicYearId, "11.50");
        StudentEnrollment invalid = enrollment(UUID.randomUUID(), classId, academicYearId, null);

        when(promotionBatchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(enrollmentRepository.findByClassIdAndStatusAndAcademicYearId(classId, EnrollmentStatus.ACTIVE, academicYearId))
                .thenReturn(List.of(valid, invalid));
        when(promotionBatchRepository.save(any(PromotionBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromotionBatch validated = promotionService.validatePromotionCriteria(batchId);

        assertEquals("VALIDATED", validated.getStatus());
        assertEquals(2, validated.getTotalProcessed());
        assertNotNull(validated.getValidationErrors());
        assertTrue(validated.getValidationErrors().contains("Moyenne finale non calculée"));
    }

    @Test
    void executePromotion_WithValidatedBatch_UpdatesPromotionCounts() {
        UUID batchId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        PromotionBatch batch = batch(batchId, classId, academicYearId);
        batch.setStatus("VALIDATED");

        StudentEnrollment promoted = enrollment(UUID.randomUUID(), classId, academicYearId, "12.50");
        StudentEnrollment retained = enrollment(UUID.randomUUID(), classId, academicYearId, "08.25");

        when(promotionBatchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(enrollmentRepository.findByClassIdAndStatusAndAcademicYearId(classId, EnrollmentStatus.ACTIVE, academicYearId))
                .thenReturn(List.of(promoted, retained));
        when(enrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promotionBatchRepository.save(any(PromotionBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromotionBatch executed = promotionService.executePromotion(batchId);

        assertEquals("EXECUTED", executed.getStatus());
        assertNotNull(executed.getExecutedAt());
        assertEquals(1, executed.getPromotedCount());
        assertEquals(1, executed.getRepeatedCount());
        assertEquals(0, executed.getGraduatedCount());
        assertEquals(PromotionStatus.PROMOTED, promoted.getPromotionStatus());
        assertEquals(PromotionStatus.RETAINED, retained.getPromotionStatus());
        verify(enrollmentRepository, times(2)).save(any(StudentEnrollment.class));
    }

    @Test
    void calculateFinalAverage_WithThreeTerms_ReturnsRoundedAverage() {
        UUID enrollmentId = UUID.randomUUID();
        UUID term1 = UUID.randomUUID();
        UUID term2 = UUID.randomUUID();
        UUID term3 = UUID.randomUUID();

        when(reportCardRepository.findByEnrollmentIdAndTermId(enrollmentId, term1))
                .thenReturn(Optional.of(reportCard("12.00")));
        when(reportCardRepository.findByEnrollmentIdAndTermId(enrollmentId, term2))
                .thenReturn(Optional.of(reportCard("14.00")));
        when(reportCardRepository.findByEnrollmentIdAndTermId(enrollmentId, term3))
                .thenReturn(Optional.of(reportCard("13.00")));

        BigDecimal result = promotionService.calculateFinalAverage(enrollmentId, List.of(term1, term2, term3));

        assertEquals(new BigDecimal("13.00"), result);
    }

    @Test
    void calculateFinalAverage_WithInvalidTermCount_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> promotionService.calculateFinalAverage(UUID.randomUUID(), List.of(UUID.randomUUID(), UUID.randomUUID())));

        assertTrue(ex.getMessage().contains("Exactly 3 term IDs"));
    }

    @Test
    void overridePromotionDecision_UpdatesEnrollmentStatus() {
        UUID enrollmentId = UUID.randomUUID();
        StudentEnrollment enrollment = enrollment(enrollmentId, UUID.randomUUID(), UUID.randomUUID(), "09.50");

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        StudentEnrollment updated = promotionService.overridePromotionDecision(
                enrollmentId, PromotionStatus.OVERRIDDEN, "Décision exceptionnelle");

        assertEquals(PromotionStatus.OVERRIDDEN, updated.getPromotionStatus());
        verify(enrollmentRepository).save(enrollment);
    }

    private AcademicYear academicYear(UUID id) {
        AcademicYear year = AcademicYear.builder()
                .label("2025-2026")
                .build();
        year.setId(id);
        return year;
    }

    private PromotionBatch batch(UUID id, UUID classId, UUID academicYearId) {
        PromotionBatch batch = PromotionBatch.builder()
                .classId(classId)
                .academicYearId(academicYearId)
                .nextAcademicYearId(UUID.randomUUID())
                .status("CREATED")
                .build();
        batch.setId(id);
        return batch;
    }

    private StudentEnrollment enrollment(UUID id, UUID classId, UUID academicYearId, String finalAverage) {
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(UUID.randomUUID())
                .classId(classId)
                .academicYearId(academicYearId)
                .status(EnrollmentStatus.ACTIVE)
                .promotionStatus(PromotionStatus.PENDING)
                .finalAverage(finalAverage != null ? new BigDecimal(finalAverage) : null)
                .build();
        enrollment.setId(id);
        return enrollment;
    }

    private ReportCard reportCard(String average) {
        ReportCard reportCard = new ReportCard();
        reportCard.setGeneralAverage(new BigDecimal(average));
        return reportCard;
    }
}

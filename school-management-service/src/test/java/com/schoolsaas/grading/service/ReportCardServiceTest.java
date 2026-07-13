package com.schoolsaas.grading.service;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.entity.ClassSubject;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    @Mock
    private ReportCardRepository reportCardRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private StudentEnrollmentRepository enrollmentRepository;
    @Mock
    private TermRepository termRepository;

    @InjectMocks
    private ReportCardService reportCardService;

    @Test
    void calculateWeightedAverage_WithMultipleSubjects_ReturnsWeightedAverage() {
        UUID enrollmentId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        ClassSubject subject1 = classSubject(2);
        ClassSubject subject2 = classSubject(3);

        when(gradeRepository.findByEnrollmentIdAndTermId(enrollmentId, termId)).thenReturn(List.of(
                grade(subject1, "10"),
                grade(subject1, "14"),
                grade(subject2, "16")
        ));

        BigDecimal result = reportCardService.calculateWeightedAverage(enrollmentId, termId);

        assertEquals(new BigDecimal("14.40"), result);
    }

    @Test
    void generateForClass_WithActiveEnrollments_UpdatesCardsAndRankings() {
        UUID classId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();

        Term term = term(termId, academicYearId);
        StudentEnrollment enrollment1 = enrollment(UUID.randomUUID(), classId, academicYearId);
        StudentEnrollment enrollment2 = enrollment(UUID.randomUUID(), classId, academicYearId);
        ReportCard card1 = reportCard(enrollment1, term);
        ReportCard card2 = reportCard(enrollment2, term);

        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(enrollmentRepository.findByClassIdAndStatusAndAcademicYearId(classId, EnrollmentStatus.ACTIVE, academicYearId))
                .thenReturn(List.of(enrollment1, enrollment2));
        when(gradeRepository.findByEnrollmentIdAndTermId(enrollment1.getId(), termId))
                .thenReturn(List.of(grade(classSubject(2), "12"), grade(classSubject(3), "18")));
        when(gradeRepository.findByEnrollmentIdAndTermId(enrollment2.getId(), termId))
                .thenReturn(List.of(grade(classSubject(2), "10"), grade(classSubject(3), "14")));
        when(reportCardRepository.findByEnrollmentIdAndTermId(enrollment1.getId(), termId)).thenReturn(Optional.of(card1));
        when(reportCardRepository.findByEnrollmentIdAndTermId(enrollment2.getId(), termId)).thenReturn(Optional.of(card2));
        when(reportCardRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(enrollment1, enrollment2));

        int generated = reportCardService.generateForClass(classId, termId);

        assertEquals(2, generated);
        assertEquals(new BigDecimal("15.60"), card1.getGeneralAverage());
        assertEquals(new BigDecimal("12.40"), card2.getGeneralAverage());
        assertEquals(2, card1.getClassSize());
        assertEquals(2, card2.getClassSize());
        assertEquals(ReportCardStatus.DRAFT, card1.getStatus());
        assertEquals(ReportCardStatus.DRAFT, card2.getStatus());
        assertEquals(1, card1.getRankInClass());
        assertEquals(2, card2.getRankInClass());
        verify(reportCardRepository, times(2)).saveAll(any());
    }

    @Test
    void generateForClass_WithoutEnrollments_ReturnsZero() {
        UUID classId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        UUID academicYearId = UUID.randomUUID();
        Term term = term(termId, academicYearId);

        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(enrollmentRepository.findByClassIdAndStatusAndAcademicYearId(classId, EnrollmentStatus.ACTIVE, academicYearId))
                .thenReturn(List.of());

        int generated = reportCardService.generateForClass(classId, termId);

        assertEquals(0, generated);
        verify(reportCardRepository, never()).saveAll(any());
    }

    @Test
    void publish_WithNullAverage_ThrowsBusinessException() {
        UUID reportCardId = UUID.randomUUID();
        ReportCard reportCard = reportCard(enrollment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                term(UUID.randomUUID(), UUID.randomUUID()));
        reportCard.setId(reportCardId);
        reportCard.setGeneralAverage(null);

        when(reportCardRepository.findById(reportCardId)).thenReturn(Optional.of(reportCard));

        BusinessException ex = assertThrows(BusinessException.class, () -> reportCardService.publish(reportCardId));

        assertEquals("REPORT_CARD_NOT_READY", ex.getCode());
    }

    @Test
    void publish_WithAverage_SetsPublishedStatusAndTimestamp() {
        UUID reportCardId = UUID.randomUUID();
        ReportCard reportCard = reportCard(enrollment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                term(UUID.randomUUID(), UUID.randomUUID()));
        reportCard.setId(reportCardId);
        reportCard.setGeneralAverage(new BigDecimal("13.75"));

        when(reportCardRepository.findById(reportCardId)).thenReturn(Optional.of(reportCard));
        when(reportCardRepository.save(reportCard)).thenReturn(reportCard);

        ReportCard published = reportCardService.publish(reportCardId);

        assertEquals(ReportCardStatus.PUBLISHED, published.getStatus());
        assertNotNull(published.getPublishedAt());
        verify(reportCardRepository).save(reportCard);
    }

    @Test
    void publishForClass_PublishesOnlyDraftCards() {
        UUID classId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        StudentEnrollment enrollment1 = enrollment(UUID.randomUUID(), classId, UUID.randomUUID());
        StudentEnrollment enrollment2 = enrollment(UUID.randomUUID(), classId, UUID.randomUUID());
        ReportCard draftCard = reportCard(enrollment1, term(termId, UUID.randomUUID()));
        ReportCard publishedCard = reportCard(enrollment2, term(termId, UUID.randomUUID()));
        draftCard.setStatus(ReportCardStatus.DRAFT);
        publishedCard.setStatus(ReportCardStatus.PUBLISHED);
        publishedCard.setPublishedAt(java.time.LocalDateTime.now());

        when(enrollmentRepository.findByClassId(classId)).thenReturn(List.of(enrollment1, enrollment2));
        when(reportCardRepository.findByEnrollmentIdAndTermId(enrollment1.getId(), termId)).thenReturn(Optional.of(draftCard));
        when(reportCardRepository.findByEnrollmentIdAndTermId(enrollment2.getId(), termId)).thenReturn(Optional.of(publishedCard));
        when(reportCardRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int count = reportCardService.publishForClass(classId, termId);

        assertEquals(1, count);
        assertEquals(ReportCardStatus.PUBLISHED, draftCard.getStatus());
        assertNotNull(draftCard.getPublishedAt());

        ArgumentCaptor<List<ReportCard>> captor = ArgumentCaptor.forClass(List.class);
        verify(reportCardRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertSame(draftCard, captor.getValue().get(0));
    }

    @Test
    void updateComments_UpdatesOnlyProvidedComments() {
        UUID reportCardId = UUID.randomUUID();
        ReportCard reportCard = reportCard(enrollment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                term(UUID.randomUUID(), UUID.randomUUID()));
        reportCard.setId(reportCardId);
        reportCard.setTeacherComment("Ancien commentaire");

        when(reportCardRepository.findById(reportCardId)).thenReturn(Optional.of(reportCard));
        when(reportCardRepository.save(reportCard)).thenReturn(reportCard);

        ReportCard updated = reportCardService.updateComments(reportCardId, null, "Très bon trimestre");

        assertEquals("Ancien commentaire", updated.getTeacherComment());
        assertEquals("Très bon trimestre", updated.getDirectorComment());
    }

    private Grade grade(ClassSubject classSubject, String value) {
        return Grade.builder()
                .classSubject(classSubject)
                .value(new BigDecimal(value))
                .evaluationDate(LocalDate.now())
                .evaluationLabel("Eval")
                .build();
    }

    private ClassSubject classSubject(int coefficient) {
        ClassSubject classSubject = ClassSubject.builder()
                .coefficient(coefficient)
                .build();
        classSubject.setId(UUID.randomUUID());
        return classSubject;
    }

    private StudentEnrollment enrollment(UUID id, UUID classId, UUID academicYearId) {
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(UUID.randomUUID())
                .classId(classId)
                .academicYearId(academicYearId)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        enrollment.setId(id);
        return enrollment;
    }

    private Term term(UUID termId, UUID academicYearId) {
        AcademicYear academicYear = AcademicYear.builder()
                .label("2025-2026")
                .build();
        academicYear.setId(academicYearId);

        Term term = Term.builder()
                .academicYear(academicYear)
                .name("Trimestre 1")
                .termNumber(1)
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 12, 20))
                .build();
        term.setId(termId);
        return term;
    }

    private ReportCard reportCard(StudentEnrollment enrollment, Term term) {
        ReportCard reportCard = ReportCard.builder()
                .enrollment(enrollment)
                .term(term)
                .status(ReportCardStatus.DRAFT)
                .build();
        reportCard.setId(UUID.randomUUID());
        return reportCard;
    }
}

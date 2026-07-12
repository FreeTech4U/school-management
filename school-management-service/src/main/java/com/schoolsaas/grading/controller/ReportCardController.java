package com.schoolsaas.grading.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.grading.dto.request.GenerateReportCardsRequest;
import com.schoolsaas.grading.dto.request.ReportCardCommentsRequest;
import com.schoolsaas.grading.dto.response.ReportCardResponse;
import com.schoolsaas.grading.entity.ReportCard;
import com.schoolsaas.grading.repository.ReportCardRepository;
import com.schoolsaas.grading.service.ReportCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for report cards (bulletins scolaires) - F-16.
 * Handles generation, publication, and retrieval of student report cards.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/school/report-cards")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final ReportCardRepository reportCardRepository;

    /**
     * F-16: POST /report-cards/generate - Generate report cards for a class/term
     * Generates report cards for all active students in a class for a specific term.
     * Requires: DIRECTOR role
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<String>> generateReportCards(
            @Valid @RequestBody GenerateReportCardsRequest request) {
        log.info("Generating report cards for class {} and term {}", request.getClassId(), request.getTermId());

        int count = reportCardService.generateForClass(request.getClassId(), request.getTermId());
        String message = String.format("Generated %d report cards successfully", count);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(message));
    }

    /**
     * F-16: GET /report-cards - List report cards with optional filters
     * Requires: DIRECTOR, TEACHER, ACCOUNTANT
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<ReportCardResponse>>> listReportCards(
            @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) UUID classId) {
        log.info("Listing report cards, termId: {}, classId: {}", termId, classId);

        List<ReportCard> reportCards = reportCardRepository.findAll();
        
        // Filter by termId if provided
        if (termId != null) {
            reportCards = reportCards.stream()
                    .filter(rc -> rc.getTerm().getId().equals(termId))
                    .collect(Collectors.toList());
        }

        List<ReportCardResponse> responses = reportCards.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Report cards retrieved successfully"));
    }

    /**
     * F-16: GET /report-cards/{id} - Retrieve a specific report card
     * Requires: DIRECTOR, TEACHER, ACCOUNTANT
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<ReportCardResponse>> getReportCard(@PathVariable UUID id) {
        log.info("Retrieving report card: {}", id);

        ReportCard reportCard = reportCardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("REPORT_CARD_NOT_FOUND", "Report card not found"));

        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(reportCard)));
    }

    /**
     * F-16: GET /report-cards/enrollment/{enrollmentId}/term/{termId} - Get report card for specific enrollment/term
     * Requires: ALL_ROLES
     */
    @GetMapping("/enrollment/{enrollmentId}/term/{termId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportCardResponse>> getEnrollmentReportCard(
            @PathVariable UUID enrollmentId, @PathVariable UUID termId) {
        log.info("Retrieving report card for enrollment {} in term {}", enrollmentId, termId);

        ReportCard reportCard = reportCardRepository.findByEnrollmentIdAndTermId(enrollmentId, termId)
                .orElseThrow(() -> BusinessException.notFound("REPORT_CARD_NOT_FOUND", 
                        "Report card not found for enrollment in this term"));

        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(reportCard)));
    }

    /**
     * F-16: PUT /report-cards/{id}/comments - Update teacher/director comments
     * Requires: DIRECTOR, TEACHER
     */
    @PutMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<ReportCardResponse>> updateComments(
            @PathVariable UUID id, @Valid @RequestBody ReportCardCommentsRequest request) {
        log.info("Updating comments for report card: {}", id);

        ReportCard updated = reportCardService.updateComments(id, 
                request.getTeacherComment(), request.getDirectorComment());

        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(updated), "Comments updated successfully"));
    }

    /**
     * F-16: POST /report-cards/{id}/publish - Publish a single report card
     * Sets status to PUBLISHED and generates PDF.
     * Requires: DIRECTOR
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<ReportCardResponse>> publishReportCard(@PathVariable UUID id) {
        log.info("Publishing report card: {}", id);

        ReportCard published = reportCardService.publish(id);

        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(published), 
                "Report card published successfully"));
    }

    /**
     * F-16: POST /report-cards/class/{classId}/term/{termId}/publish-all - Publish all report cards for a class/term
     * Bulk publishes all DRAFT report cards.
     * Requires: DIRECTOR
     */
    @PostMapping("/class/{classId}/term/{termId}/publish-all")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<String>> publishForClass(
            @PathVariable UUID classId, @PathVariable UUID termId) {
        log.info("Publishing all report cards for class {} in term {}", classId, termId);

        int count = reportCardService.publishForClass(classId, termId);
        String message = String.format("Published %d report cards successfully", count);

        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    /**
     * F-16: GET /report-cards/{id}/pdf - Download report card as PDF
     * Streams PDF file to client.
     * Requires: ALL_ROLES
     * TODO: Implement PDF generation in Phase 5
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getReportCardPdf(@PathVariable UUID id) {
        log.info("Retrieving PDF for report card: {}", id);

        ReportCard reportCard = reportCardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("REPORT_CARD_NOT_FOUND", "Report card not found"));

        // TODO: In Phase 5, implement actual PDF generation using Thymeleaf + OpenHTMLtoPDF
        // For now, return placeholder
        return ResponseEntity.ok(ApiResponse.ok(reportCard.getPdfUrl() != null ? reportCard.getPdfUrl() : "PDF not yet generated"));
    }

    private ReportCardResponse mapToResponse(ReportCard reportCard) {
        return ReportCardResponse.builder()
                .id(reportCard.getId())
                .enrollmentId(reportCard.getEnrollment().getId())
                .studentName(reportCard.getEnrollment().getStudentId().toString()) // TODO: fetch actual student name
                .termId(reportCard.getTerm().getId())
                .termName(reportCard.getTerm().getName())
                .generalAverage(reportCard.getGeneralAverage())
                .rankInClass(reportCard.getRankInClass())
                .classSize(reportCard.getClassSize())
                .teacherComment(reportCard.getTeacherComment())
                .directorComment(reportCard.getDirectorComment())
                .status(reportCard.getStatus())
                .pdfUrl(reportCard.getPdfUrl())
                .publishedAt(reportCard.getPublishedAt())
                .createdAt(reportCard.getCreatedAt())
                .build();
    }
}

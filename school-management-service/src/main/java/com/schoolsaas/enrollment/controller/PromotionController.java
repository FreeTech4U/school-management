package com.schoolsaas.enrollment.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.dto.request.CreatePromotionBatchRequest;
import com.schoolsaas.enrollment.dto.response.PromotionBatchResponse;
import com.schoolsaas.enrollment.entity.PromotionBatch;
import com.schoolsaas.enrollment.repository.PromotionBatchRepository;
import com.schoolsaas.enrollment.service.PromotionService;
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
 * Controller for student promotion management - F-19.
 * Handles creation, validation, and execution of promotion batches.
 * Supports promotion to next class, retention in same class, and graduation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/school/promotion-batches")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final PromotionBatchRepository promotionBatchRepository;

    /**
     * F-19: POST /promotion-batches - Create a new promotion batch
     * Creates a batch for promoting students from one academic year to the next.
     * Initial status is CREATED.
     * Requires: DIRECTOR role
     */
    @PostMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<PromotionBatchResponse>> createPromotionBatch(
            @Valid @RequestBody CreatePromotionBatchRequest request) {
        log.info("Creating promotion batch for class {} from AY {} to AY {}", 
                request.getClassId(), request.getAcademicYearId(), request.getNextAcademicYearId());

        PromotionBatch batch = promotionService.createPromotionBatch(
                request.getClassId(), 
                request.getAcademicYearId(), 
                request.getNextAcademicYearId(), 
                request.getNotes());

        PromotionBatchResponse response = mapToResponse(batch);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Promotion batch created successfully"));
    }

    /**
     * F-19: PUT /promotion-batches/{id}/validate - Validate promotion criteria
     * Checks that all students have final averages calculated.
     * Updates status to VALIDATED.
     * Requires: DIRECTOR role
     */
    @PutMapping("/{id}/validate")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<PromotionBatchResponse>> validatePromotionBatch(
            @PathVariable UUID id) {
        log.info("Validating promotion batch: {}", id);

        PromotionBatch validated = promotionService.validatePromotionCriteria(id);
        PromotionBatchResponse response = mapToResponse(validated);

        return ResponseEntity.ok(ApiResponse.ok(response, "Promotion batch validated successfully"));
    }

    /**
     * F-19: PUT /promotion-batches/{id}/execute - Execute promotion
     * Applies promotion criteria to all students:
     * - finalAverage >= passingAverage → PROMOTED to next class
     * - finalAverage < passingAverage → RETAINED in same class
     * - Last class → GRADUATED
     * Updates status to EXECUTED.
     * Requires: DIRECTOR role
     */
    @PutMapping("/{id}/execute")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<PromotionBatchResponse>> executePromotion(
            @PathVariable UUID id) {
        log.info("Executing promotion batch: {}", id);

        PromotionBatch executed = promotionService.executePromotion(id);
        PromotionBatchResponse response = mapToResponse(executed);

        return ResponseEntity.ok(ApiResponse.ok(response, "Promotion executed successfully"));
    }

    /**
     * F-19: GET /promotion-batches - List promotion batches
     * Lists all promotion batches with optional filtering by academic year or status.
     * Requires: DIRECTOR role
     */
    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<List<PromotionBatchResponse>>> listPromotionBatches(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) String status) {
        log.info("Listing promotion batches, academicYearId: {}, status: {}", academicYearId, status);

        List<PromotionBatch> batches;
        
        if (academicYearId != null && status != null) {
            batches = promotionBatchRepository.findByAcademicYearIdAndStatus(academicYearId, status);
        } else if (academicYearId != null) {
            batches = promotionBatchRepository.findByAcademicYearIdAndStatus(academicYearId, "CREATED");
        } else {
            batches = promotionBatchRepository.findAll();
        }

        List<PromotionBatchResponse> responses = batches.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Promotion batches retrieved successfully"));
    }

    /**
     * GET /promotion-batches/{id} - Retrieve a specific promotion batch
     * Requires: DIRECTOR role
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<PromotionBatchResponse>> getPromotionBatch(
            @PathVariable UUID id) {
        log.info("Retrieving promotion batch: {}", id);

        PromotionBatch batch = promotionBatchRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PROMOTION_BATCH_NOT_FOUND", 
                        "Promotion batch not found"));

        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(batch)));
    }

    private PromotionBatchResponse mapToResponse(PromotionBatch batch) {
        return PromotionBatchResponse.builder()
                .id(batch.getId())
                .classId(batch.getClassId())
                .academicYearId(batch.getAcademicYearId())
                .nextAcademicYearId(batch.getNextAcademicYearId())
                .status(batch.getStatus())
                .promotedCount(batch.getPromotedCount())
                .repeatedCount(batch.getRepeatedCount())
                .graduatedCount(batch.getGraduatedCount())
                .totalProcessed(batch.getTotalProcessed())
                .validationErrors(batch.getValidationErrors())
                .executedAt(batch.getExecutedAt())
                .notes(batch.getNotes())
                .directorComment(batch.getDirectorComment())
                .createdAt(batch.getCreatedAt())
                .build();
    }
}

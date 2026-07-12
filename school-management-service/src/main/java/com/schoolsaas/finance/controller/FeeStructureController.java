package com.schoolsaas.finance.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.finance.dto.request.FeeStructureRequest;
import com.schoolsaas.finance.dto.request.StudentFeeDiscountRequest;
import com.schoolsaas.finance.dto.response.FeeStructureResponse;
import com.schoolsaas.finance.dto.response.StudentFeeResponse;
import com.schoolsaas.finance.dto.response.StudentFeeSummaryResponse;
import com.schoolsaas.finance.entity.FeeStructure;
import com.schoolsaas.finance.entity.StudentFee;
import com.schoolsaas.finance.repository.FeeStructureRepository;
import com.schoolsaas.finance.repository.StudentFeeRepository;
import com.schoolsaas.finance.service.FeeStructureService;
import com.schoolsaas.finance.service.StudentFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for fee structure and student fees management (F-10, F-11).
 * Handles creation, retrieval, and modification of fee structures and student fees.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService feeStructureService;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final StudentFeeService studentFeeService;

    /**
     * F-10: POST /fee-structures - Create a new fee structure
     * Requires: DIRECTOR role
     */
    @PostMapping("/fee-structures")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> createFeeStructure(
            @Valid @RequestBody FeeStructureRequest request) {
        log.info("Creating fee structure: {}", request.getLabel());

        FeeStructure feeStructure = FeeStructure.builder()
                .academicYearId(request.getAcademicYearId())
                .classId(request.getClassId())
                .feeType(request.getFeeType())
                .label(request.getLabel())
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .installmentsAllowed(request.getInstallmentsAllowed())
                .maxInstallments(request.getMaxInstallments())
                .build();

        FeeStructure saved = feeStructureRepository.save(feeStructure);
        FeeStructureResponse response = mapToResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Fee structure created successfully"));
    }

    /**
     * F-10: GET /fee-structures - List all fee structures (with optional filters)
     * Requires: DIRECTOR, ACCOUNTANT
     */
    @GetMapping("/fee-structures")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<FeeStructureResponse>>> listFeeStructures(
            @RequestParam(required = false) UUID academicYearId) {
        log.info("Listing fee structures, academicYearId: {}", academicYearId);

        List<FeeStructure> structures = academicYearId != null 
                ? feeStructureRepository.findByAcademicYearId(academicYearId)
                : feeStructureRepository.findAll();

        List<FeeStructureResponse> responses = structures.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Fee structures retrieved successfully"));
    }

    /**
     * F-10: GET /fee-structures/{id} - Retrieve a specific fee structure
     * Requires: DIRECTOR, ACCOUNTANT
     */
    @GetMapping("/fee-structures/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> getFeeStructure(@PathVariable UUID id) {
        log.info("Retrieving fee structure: {}", id);

        FeeStructure structure = feeStructureRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("FEE_STRUCTURE_NOT_FOUND", "Fee structure not found"));

        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(structure)));
    }

    /**
     * F-10: PUT /fee-structures/{id} - Update a fee structure
     * Requires: DIRECTOR
     */
    @PutMapping("/fee-structures/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<FeeStructureResponse>> updateFeeStructure(
            @PathVariable UUID id, @Valid @RequestBody FeeStructureRequest request) {
        log.info("Updating fee structure: {}", id);

        FeeStructure structure = feeStructureRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("FEE_STRUCTURE_NOT_FOUND", "Fee structure not found"));

        structure.setLabel(request.getLabel());
        structure.setAmount(request.getAmount());
        structure.setDueDate(request.getDueDate());
        structure.setFeeType(request.getFeeType());
        structure.setInstallmentsAllowed(request.getInstallmentsAllowed());
        structure.setMaxInstallments(request.getMaxInstallments());

        FeeStructure updated = feeStructureRepository.save(structure);
        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(updated), "Fee structure updated successfully"));
    }

    /**
     * F-10: DELETE /fee-structures/{id} - Delete a fee structure
     * Requires: DIRECTOR
     */
    @DeleteMapping("/fee-structures/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteFeeStructure(@PathVariable UUID id) {
        log.info("Deleting fee structure: {}", id);

        FeeStructure structure = feeStructureRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("FEE_STRUCTURE_NOT_FOUND", "Fee structure not found"));

        feeStructureRepository.delete(structure);
        return ResponseEntity.ok(ApiResponse.ok(null, "Fee structure deleted successfully"));
    }

    /**
     * F-11: PUT /student-fees/{id}/discount - Apply discount to a student fee
     * Requires: DIRECTOR
     */
    @PutMapping("/student-fees/{id}/discount")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<StudentFeeResponse>> applyDiscount(
            @PathVariable UUID id, @Valid @RequestBody StudentFeeDiscountRequest request) {
        log.info("Applying discount to student fee: {}", id);

        studentFeeService.applyDiscount(id, request.getDiscountAmount(), request.getDiscountReason());

        StudentFee fee = studentFeeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("FEE_NOT_FOUND", "Student fee not found"));

        return ResponseEntity.ok(ApiResponse.ok(mapStudentFeeToResponse(fee), "Discount applied successfully"));
    }

    /**
     * F-11: GET /enrollments/{enrollmentId}/fees - List fees for a student enrollment
     * Requires: DIRECTOR, ACCOUNTANT
     */
    @GetMapping("/enrollments/{enrollmentId}/fees")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<StudentFeeResponse>>> listEnrollmentFees(
            @PathVariable UUID enrollmentId) {
        log.info("Listing fees for enrollment: {}", enrollmentId);

        List<StudentFee> fees = studentFeeRepository.findByEnrollmentId(enrollmentId);
        List<StudentFeeResponse> responses = fees.stream()
                .map(this::mapStudentFeeToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Student fees retrieved successfully"));
    }

    /**
     * F-11: GET /students/{studentId}/fees/summary - Get fee summary for a student
     * Requires: DIRECTOR, ACCOUNTANT
     */
    @GetMapping("/students/{studentId}/fees/summary")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<StudentFeeSummaryResponse>> getStudentFeeSummary(
            @PathVariable UUID studentId) {
        log.info("Getting fee summary for student: {}", studentId);

        // TODO: Implement in Phase 5 - requires StudentEnrollment lookup
        StudentFeeSummaryResponse summary = StudentFeeSummaryResponse.builder()
                .studentName("TODO")
                .totalDue(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .amountRemaining(BigDecimal.ZERO)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    private FeeStructureResponse mapToResponse(FeeStructure structure) {
        return FeeStructureResponse.builder()
                .id(structure.getId())
                .academicYearId(structure.getAcademicYearId())
                .classId(structure.getClassId())
                .feeType(structure.getFeeType())
                .label(structure.getLabel())
                .amount(structure.getAmount())
                .dueDate(structure.getDueDate())
                .installmentsAllowed(structure.getInstallmentsAllowed())
                .maxInstallments(structure.getMaxInstallments())
                .createdAt(structure.getCreatedAt())
                .updatedAt(structure.getUpdatedAt())
                .build();
    }

    private StudentFeeResponse mapStudentFeeToResponse(StudentFee fee) {
        return StudentFeeResponse.builder()
                .id(fee.getId())
                .enrollmentId(fee.getEnrollment().getId())
                .feeStructureId(fee.getFeeStructure().getId())
                .feeLabel(fee.getFeeStructure().getLabel())
                .amountDue(fee.getAmountDue())
                .amountPaid(fee.getAmountPaid())
                .discountAmount(fee.getDiscountAmount())
                .discountReason(fee.getDiscountReason())
                .dueDate(fee.getDueDate())
                .status(fee.getStatus())
                .createdAt(fee.getCreatedAt())
                .build();
    }
}

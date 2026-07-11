package com.schoolsaas.finance.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.finance.entity.FeeStructure;
import com.schoolsaas.finance.service.FeeStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService feeStructureService;

    @GetMapping("/fee-structures")
    public ApiResponse<List<FeeStructure>> getAll() {
        return ApiResponse.ok(feeStructureService.getAllFeeStructures());
    }

    @GetMapping("/academic-years/{yearId}/fee-structures")
    public ApiResponse<List<FeeStructure>> getByYear(@PathVariable UUID yearId) {
        return ApiResponse.ok(feeStructureService.getByYear(yearId));
    }

    @PostMapping("/fee-structures")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<FeeStructure> create(@RequestBody FeeStructure feeStructure) {
        return ApiResponse.ok(feeStructureService.createFeeStructure(feeStructure));
    }

    @PutMapping("/fee-structures/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<FeeStructure> update(@PathVariable UUID id, @RequestBody FeeStructure details) {
        return ApiResponse.ok(feeStructureService.updateFeeStructure(id, details));
    }

    @DeleteMapping("/fee-structures/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        feeStructureService.deleteFeeStructure(id);
        return ApiResponse.ok(null, "Structure de frais supprimée");
    }
}

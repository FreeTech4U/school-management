package com.schoolsaas.academic.controller;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.service.AcademicYearService;
import com.schoolsaas.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping("/academic-years")
    public ApiResponse<List<AcademicYear>> getAllYears() {
        return ApiResponse.ok(academicYearService.getAllYears());
    }

    @GetMapping("/academic-years/current")
    public ApiResponse<AcademicYear> getCurrentYear() {
        return ApiResponse.ok(academicYearService.getCurrentYear());
    }

    @PostMapping("/academic-years")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<AcademicYear> createYear(@RequestBody AcademicYear year) {
        return ApiResponse.ok(academicYearService.createYear(year));
    }

    @PutMapping("/academic-years/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<AcademicYear> updateYear(@PathVariable UUID id, @RequestBody AcademicYear year) {
        return ApiResponse.ok(academicYearService.updateYear(id, year));
    }

    @PostMapping("/academic-years/{id}/close")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> closeYear(@PathVariable UUID id) {
        academicYearService.closeYear(id);
        return ApiResponse.ok(null, "Année scolaire clôturée");
    }

    // Terms
    @GetMapping("/academic-years/{yearId}/terms")
    public ApiResponse<List<Term>> getTerms(@PathVariable UUID yearId) {
        return ApiResponse.ok(academicYearService.getTermsByYear(yearId));
    }

    @PostMapping("/academic-years/{yearId}/terms")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Term> createTerm(@PathVariable UUID yearId, @RequestBody Term term) {
        return ApiResponse.ok(academicYearService.createTerm(yearId, term));
    }

    @PostMapping("/terms/{id}/open-grades-entry")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> openGradesEntry(@PathVariable UUID id) {
        academicYearService.setGradesEntryStatus(id, true);
        return ApiResponse.ok(null, "Saisie des notes ouverte");
    }

    @PostMapping("/terms/{id}/close-grades-entry")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> closeGradesEntry(@PathVariable UUID id) {
        academicYearService.setGradesEntryStatus(id, false);
        return ApiResponse.ok(null, "Saisie des notes fermée");
    }
}

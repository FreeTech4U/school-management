package com.schoolsaas.enrollment.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.enrollment.dto.request.EnrollStudentRequest;
import com.schoolsaas.enrollment.dto.response.EnrollmentResponse;
import com.schoolsaas.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/enrollments")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ApiResponse<EnrollmentResponse> enroll(@Valid @RequestBody EnrollStudentRequest request) {
        return ApiResponse.ok(enrollmentService.enroll(request));
    }

    @GetMapping("/classes/{classId}/enrollments")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ApiResponse<List<EnrollmentResponse>> getEnrollmentsByClass(@PathVariable UUID classId) {
        return ApiResponse.ok(enrollmentService.getEnrollmentsByClass(classId));
    }

    @PutMapping("/enrollments/{id}/transfer")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> transfer(@PathVariable UUID id, @RequestParam String reason) {
        enrollmentService.transferStudent(id, reason);
        return ApiResponse.ok(null, "Transfert enregistré");
    }

    @PutMapping("/enrollments/{id}/withdraw")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> withdraw(@PathVariable UUID id) {
        enrollmentService.withdrawStudent(id);
        return ApiResponse.ok(null, "Retrait enregistré");
    }
}

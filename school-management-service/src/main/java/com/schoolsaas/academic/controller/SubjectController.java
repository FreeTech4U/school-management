package com.schoolsaas.academic.controller;

import com.schoolsaas.academic.entity.Subject;
import com.schoolsaas.academic.service.SubjectService;
import com.schoolsaas.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ApiResponse<List<Subject>> getAllSubjects() {
        return ApiResponse.ok(subjectService.getAllSubjects());
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Subject> createSubject(@RequestBody Subject subject) {
        return ApiResponse.ok(subjectService.createSubject(subject));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Subject> updateSubject(@PathVariable UUID id, @RequestBody Subject subject) {
        return ApiResponse.ok(subjectService.updateSubject(id, subject));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> deleteSubject(@PathVariable UUID id) {
        subjectService.deleteSubject(id);
        return ApiResponse.ok(null, "Matière désactivée");
    }
}

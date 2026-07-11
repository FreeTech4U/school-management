package com.schoolsaas.enrollment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.enrollment.dto.request.CreateStudentRequest;
import com.schoolsaas.enrollment.dto.response.StudentResponse;
import com.schoolsaas.enrollment.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Endpoints pour la gestion des élèves")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'ACCOUNTANT')")
    @Operation(summary = "Lister les élèves", description = "Récupère une liste paginée des élèves, avec possibilité de recherche par nom")
    public ApiResponse<Page<StudentResponse>> getAllStudents(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<StudentResponse> page = studentService.getAllStudents(search, pageable);
        return ApiResponse.paged(page, ApiResponse.PageMeta.of(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'ACCOUNTANT')")
    public ApiResponse<StudentResponse> getStudentById(@PathVariable UUID id) {
        return ApiResponse.ok(studentService.getStudentById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    @Operation(summary = "Créer un élève", description = "Enregistre un nouvel élève dans l'établissement")
    public ApiResponse<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return ApiResponse.ok(studentService.createStudent(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'ACCOUNTANT')")
    public ApiResponse<StudentResponse> updateStudent(@PathVariable UUID id, @Valid @RequestBody CreateStudentRequest request) {
        return ApiResponse.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> deleteStudent(@PathVariable UUID id) {
        studentService.deleteStudent(id);
        return ApiResponse.ok(null, "Élève désactivé avec succès");
    }
}

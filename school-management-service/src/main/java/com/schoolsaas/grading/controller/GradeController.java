package com.schoolsaas.grading.controller;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.repository.ClassSubjectRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import com.schoolsaas.grading.dto.request.BulkGradeRequest;
import com.schoolsaas.grading.dto.request.GradeRequest;
import com.schoolsaas.grading.dto.response.GradeResponse;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.repository.GradeRepository;
import com.schoolsaas.grading.service.GradeService;
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
 * Controller for grade management (saisie des notes) - F-15.
 * Handles creation, retrieval, and modification of student grades.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/school/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final GradeRepository gradeRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final TermRepository termRepository;

    /**
     * F-15: POST /grades - Record a single grade
     * Requires: DIRECTOR, TEACHER
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<GradeResponse>> recordGrade(
            @Valid @RequestBody GradeRequest request) {
        log.info("Recording grade for enrollment: {}", request.getEnrollmentId());

        StudentEnrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> BusinessException.notFound("ENROLLMENT_NOT_FOUND", "Student enrollment not found"));
        
        ClassSubject classSubject = classSubjectRepository.findById(request.getClassSubjectId())
                .orElseThrow(() -> BusinessException.notFound("CLASS_SUBJECT_NOT_FOUND", "Class subject not found"));
        
        Term term = termRepository.findById(request.getTermId())
                .orElseThrow(() -> BusinessException.notFound("TERM_NOT_FOUND", "Term not found"));

        Grade grade = Grade.builder()
                .enrollment(enrollment)
                .classSubject(classSubject)
                .term(term)
                .value(request.getValue())
                .evaluationType(com.schoolsaas.common.enums.EvaluationType.valueOf(request.getEvaluationType().toUpperCase()))
                .evaluationLabel(request.getEvaluationLabel())
                .evaluationDate(request.getEvaluationDate())
                .comment(request.getComment())
                .build();

        Grade saved = gradeService.enterGrade(grade);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mapToResponse(saved), "Grade recorded successfully"));
    }

    /**
     * F-15: POST /grades/bulk - Record multiple grades for a class
     * Bulk entry for entire class during a single grading session.
     * Requires: DIRECTOR, TEACHER
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> recordBulkGrades(
            @Valid @RequestBody BulkGradeRequest request) {
        log.info("Recording bulk grades for classSubject: {}", request.getClassSubjectId());

        ClassSubject classSubject = classSubjectRepository.findById(request.getClassSubjectId())
                .orElseThrow(() -> BusinessException.notFound("CLASS_SUBJECT_NOT_FOUND", "Class subject not found"));
        
        Term term = termRepository.findById(request.getTermId())
                .orElseThrow(() -> BusinessException.notFound("TERM_NOT_FOUND", "Term not found"));

        int count = 0;
        for (BulkGradeRequest.BulkGradeItem item : request.getGrades()) {
            StudentEnrollment enrollment = enrollmentRepository.findById(item.getEnrollmentId())
                    .orElseThrow(() -> BusinessException.notFound("ENROLLMENT_NOT_FOUND", "Student enrollment not found"));
            
            Grade grade = Grade.builder()
                    .enrollment(enrollment)
                    .classSubject(classSubject)
                    .term(term)
                    .value(item.getValue())
                    .evaluationType(com.schoolsaas.common.enums.EvaluationType.valueOf(request.getEvaluationType().toUpperCase()))
                    .evaluationLabel(request.getEvaluationLabel())
                    .evaluationDate(request.getEvaluationDate())
                    .comment(item.getComment())
                    .build();
            gradeService.enterGrade(grade);
            count++;
        }

        String message = String.format("Recorded %d grades successfully", count);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(message));
    }

    /**
     * F-15: GET /grades - List grades with optional filters
     * Requires: DIRECTOR, TEACHER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<GradeResponse>>> listGrades(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID termId) {
        log.info("Listing grades, classId: {}, subjectId: {}, termId: {}", classId, subjectId, termId);

        List<Grade> grades = gradeRepository.findAll();
        
        // Filter by termId if provided
        if (termId != null) {
            grades = grades.stream()
                    .filter(g -> g.getTerm() != null && g.getTerm().getId().equals(termId))
                    .collect(Collectors.toList());
        }

        List<GradeResponse> responses = grades.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Grades retrieved successfully"));
    }

    /**
     * F-15: GET /grades/enrollment/{enrollmentId} - Get grades for a student
     * Requires: DIRECTOR, TEACHER
     */
    @GetMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<GradeResponse>>> getEnrollmentGrades(
            @PathVariable UUID enrollmentId,
            @RequestParam(required = false) UUID termId) {
        log.info("Retrieving grades for enrollment: {}, termId: {}", enrollmentId, termId);

        List<Grade> grades = gradeRepository.findAll().stream()
                .filter(g -> g.getEnrollment() != null && g.getEnrollment().getId().equals(enrollmentId))
                .collect(Collectors.toList());
        
        if (termId != null) {
            grades = grades.stream()
                    .filter(g -> g.getTerm() != null && g.getTerm().getId().equals(termId))
                    .collect(Collectors.toList());
        }

        List<GradeResponse> responses = grades.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Enrollment grades retrieved successfully"));
    }

    /**
     * F-15: PUT /grades/{id} - Update a grade
     * Requires: DIRECTOR, TEACHER
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<GradeResponse>> updateGrade(
            @PathVariable UUID id, @Valid @RequestBody GradeRequest request) {
        log.info("Updating grade: {}", id);

        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("GRADE_NOT_FOUND", "Grade not found"));

        grade.setValue(request.getValue());
        grade.setEvaluationLabel(request.getEvaluationLabel());
        grade.setEvaluationDate(request.getEvaluationDate());
        grade.setComment(request.getComment());

        Grade updated = gradeRepository.save(grade);
        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(updated), "Grade updated successfully"));
    }

    /**
     * F-15: DELETE /grades/{id} - Delete a grade
     * Requires: DIRECTOR
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteGrade(@PathVariable UUID id) {
        log.info("Deleting grade: {}", id);

        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("GRADE_NOT_FOUND", "Grade not found"));

        gradeRepository.delete(grade);
        return ResponseEntity.ok(ApiResponse.ok(null, "Grade deleted successfully"));
    }

    private GradeResponse mapToResponse(Grade grade) {
        return GradeResponse.builder()
                .id(grade.getId())
                .enrollmentId(grade.getEnrollment() != null ? grade.getEnrollment().getId() : null)
                .classSubjectId(grade.getClassSubject() != null ? grade.getClassSubject().getId() : null)
                .termId(grade.getTerm() != null ? grade.getTerm().getId() : null)
                .value(grade.getValue())
                .evaluationType(grade.getEvaluationType() != null ? grade.getEvaluationType().toString() : "UNKNOWN")
                .evaluationLabel(grade.getEvaluationLabel())
                .evaluationDate(grade.getEvaluationDate())
                .comment(grade.getComment())
                .createdAt(grade.getCreatedAt())
                .updatedAt(grade.getUpdatedAt())
                .build();
    }
}

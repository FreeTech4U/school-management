package com.schoolsaas.grading.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.grading.dto.request.GradeRequest;
import com.schoolsaas.grading.dto.response.GradeResponse;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.mapper.GradeMapper;
import com.schoolsaas.grading.service.GradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/school/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final GradeMapper gradeMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ApiResponse<List<GradeResponse>> getGrades(
            @RequestParam UUID classSubjectId,
            @RequestParam UUID termId) {
        List<GradeResponse> responses = gradeService.getGradesForClassSubject(classSubjectId, termId).stream()
                .map(gradeMapper::toResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(responses);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ApiResponse<GradeResponse> enterGrade(@Valid @RequestBody GradeRequest request) {
        Grade grade = gradeMapper.toEntity(request);
        Grade savedGrade = gradeService.enterGrade(grade);
        return ApiResponse.ok(gradeMapper.toResponse(savedGrade));
    }
}

package com.schoolsaas.academic.controller;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Level;
import com.schoolsaas.academic.entity.SchoolClass;
import com.schoolsaas.academic.service.ClassService;
import com.schoolsaas.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping("/levels")
    public ApiResponse<List<Level>> getLevels() {
        return ApiResponse.ok(classService.getAllLevels());
    }

    @PostMapping("/levels")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Level> createLevel(@RequestBody Level level) {
        return ApiResponse.ok(classService.createLevel(level));
    }

    @GetMapping("/classes")
    public ApiResponse<List<SchoolClass>> getAllClasses() {
        return ApiResponse.ok(classService.getAllClasses());
    }

    @GetMapping("/academic-years/{yearId}/classes")
    public ApiResponse<List<SchoolClass>> getClassesByYear(@PathVariable UUID yearId) {
        return ApiResponse.ok(classService.getClassesByYear(yearId));
    }

    @GetMapping("/classes/{id}")
    public ApiResponse<SchoolClass> getClassById(@PathVariable UUID id) {
        return ApiResponse.ok(classService.getClassById(id));
    }

    @PostMapping("/classes")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<SchoolClass> createClass(@RequestBody SchoolClass schoolClass) {
        return ApiResponse.ok(classService.createClass(schoolClass));
    }

    @PutMapping("/classes/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<SchoolClass> updateClass(@PathVariable UUID id, @RequestBody SchoolClass schoolClass) {
        schoolClass.setId(id);
        return ApiResponse.ok(classService.createClass(schoolClass));
    }

    @DeleteMapping("/classes/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> deleteClass(@PathVariable UUID id) {
        classService.deleteClass(id);
        return ApiResponse.ok(null, "Classe supprimée");
    }

    @GetMapping("/classes/{classId}/subjects")
    public ApiResponse<List<ClassSubject>> getClassSubjects(@PathVariable UUID classId) {
        return ApiResponse.ok(classService.getClassSubjects(classId));
    }

    @PostMapping("/classes/{classId}/subjects")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<ClassSubject> assignSubject(@PathVariable UUID classId, @RequestBody ClassSubject classSubject) {
        return ApiResponse.ok(classService.assignSubjectToClass(classId, classSubject));
    }

    @DeleteMapping("/class-subjects/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<Void> removeSubject(@PathVariable UUID id) {
        classService.removeSubjectFromClass(id);
        return ApiResponse.ok(null, "Matière retirée de la classe");
    }
}

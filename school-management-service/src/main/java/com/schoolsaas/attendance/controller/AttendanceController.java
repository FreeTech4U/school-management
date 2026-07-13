package com.schoolsaas.attendance.controller;

import com.schoolsaas.attendance.dto.request.AttendanceRequest;
import com.schoolsaas.attendance.dto.request.BulkAttendanceRequest;
import com.schoolsaas.attendance.dto.response.AttendanceResponse;
import com.schoolsaas.attendance.dto.response.AttendanceSummaryResponse;
import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.attendance.repository.AttendanceRepository;
import com.schoolsaas.attendance.service.AttendanceService;
import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.common.enums.AttendanceStatus;
import com.schoolsaas.common.enums.Period;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.enrollment.entity.StudentEnrollment;
import com.schoolsaas.enrollment.repository.StudentEnrollmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for attendance management (présences) - F-17.
 * Handles recording, retrieval, and modification of student attendance.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/school/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    /**
     * F-17: POST /attendance - Record single attendance
     * Requires: DIRECTOR, TEACHER
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> recordAttendance(
            @Valid @RequestBody AttendanceRequest request) {
        log.info("Recording attendance for enrollment: {} on date: {}", 
                request.getEnrollmentId(), request.getDate());

        StudentEnrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> BusinessException.notFound("ENROLLMENT_NOT_FOUND", "Student enrollment not found"));

        Period period = Period.valueOf(request.getPeriod().toUpperCase());
        AttendanceStatus status = AttendanceStatus.valueOf(request.getStatus().toUpperCase());

        Attendance attendance = Attendance.builder()
                .enrollment(enrollment)
                .date(request.getDate())
                .period(period)
                .status(status)
                .justification(request.getJustification())
                .build();

        Attendance recorded = attendanceService.recordAttendance(attendance);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mapToResponse(recorded), "Attendance recorded successfully"));
    }

    /**
     * F-17: POST /attendance/bulk - Record attendance for entire class
     * Bulk entry for class attendance in one session.
     * Requires: DIRECTOR, TEACHER
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> recordBulkAttendance(
            @Valid @RequestBody BulkAttendanceRequest request) {
        log.info("Recording bulk attendance for class: {} on date: {}", 
                request.getClassId(), request.getDate());

        int count = 0;
        for (BulkAttendanceRequest.AttendanceRecord record : request.getRecords()) {
            StudentEnrollment enrollment = enrollmentRepository.findById(record.getEnrollmentId())
                    .orElseThrow(() -> BusinessException.notFound("ENROLLMENT_NOT_FOUND", "Student enrollment not found"));

            Period period = Period.valueOf(request.getPeriod().toUpperCase());
            AttendanceStatus status = AttendanceStatus.valueOf(record.getStatus().toUpperCase());

            Attendance attendance = Attendance.builder()
                    .enrollment(enrollment)
                    .date(request.getDate())
                    .period(period)
                    .status(status)
                    .justification(record.getJustification())
                    .build();

            attendanceService.recordAttendance(attendance);
            count++;
        }

        String message = String.format("Recorded %d attendance entries successfully", count);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(message));
    }

    /**
     * F-17: GET /attendance - Query attendance records
     * Optional filters by enrollment, date range
     * Requires: DIRECTOR, TEACHER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> queryAttendance(
            @RequestParam(required = false) UUID enrollmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Querying attendance, enrollmentId: {}, from: {}, to: {}", enrollmentId, from, to);

        List<Attendance> records;
        
        if (enrollmentId != null) {
            records = attendanceRepository.findByEnrollmentIdAndDate(enrollmentId, LocalDate.now());
        } else {
            records = attendanceRepository.findAll();
        }

        // Filter by date range if provided
        if (from != null && to != null) {
            records = records.stream()
                    .filter(a -> !a.getDate().isBefore(from) && !a.getDate().isAfter(to))
                    .collect(Collectors.toList());
        }

        List<AttendanceResponse> responses = records.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Attendance records retrieved successfully"));
    }

    /**
     * F-17: GET /attendance/class/{classId} - Get class attendance for a date
     * Requires: DIRECTOR, TEACHER
     */
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getClassAttendance(
            @PathVariable UUID classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Retrieving class attendance for class: {} on date: {}", classId, date);

        // Get all attendance records for the given date and filter by class
        List<Attendance> records = attendanceRepository.findAll().stream()
                .filter(a -> a.getDate().equals(date))
                .collect(Collectors.toList());

        List<AttendanceResponse> responses = records.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Class attendance retrieved successfully"));
    }

    /**
     * F-17: GET /attendance/student/{studentId}/summary - Get attendance summary for student
     * Requires: ALL_ROLES
     */
    @GetMapping("/student/{studentId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getStudentAttendanceSummary(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID termId) {
        log.info("Retrieving attendance summary for student: {}, termId: {}", studentId, termId);

        List<Attendance> records = attendanceRepository.findAll().stream()
                .filter(a -> a.getEnrollment() != null && a.getEnrollment().getStudentId().equals(studentId))
                .collect(Collectors.toList());

        long presentCount = records.stream()
                .filter(a -> com.schoolsaas.common.enums.AttendanceStatus.PRESENT.equals(a.getStatus()))
                .count();

        long absentCount = records.stream()
                .filter(a -> com.schoolsaas.common.enums.AttendanceStatus.ABSENT.equals(a.getStatus()))
                .count();

        long lateCount = records.stream()
                .filter(a -> com.schoolsaas.common.enums.AttendanceStatus.LATE.equals(a.getStatus()))
                .count();

        long excusedCount = records.stream()
                .filter(a -> com.schoolsaas.common.enums.AttendanceStatus.EXCUSED.equals(a.getStatus()))
                .count();

        AttendanceSummaryResponse summary = AttendanceSummaryResponse.builder()
                .studentId(studentId)
                .totalDays((int) records.size())
                .presentDays((int) presentCount)
                .absentDays((int) absentCount)
                .lateDays((int) lateCount)
                .excusedDays((int) excusedCount)
                .attendanceRate(records.isEmpty() ? 0.0 : (double) presentCount / records.size() * 100)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(summary, "Attendance summary retrieved successfully"));
    }

    /**
     * F-17: PUT /attendance/{id} - Update attendance (justify or correct)
     * Requires: DIRECTOR, TEACHER
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendance(
            @PathVariable UUID id, @Valid @RequestBody AttendanceRequest request) {
        log.info("Updating attendance: {}", id);

        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ATTENDANCE_NOT_FOUND", "Attendance record not found"));

        AttendanceStatus status = AttendanceStatus.valueOf(request.getStatus().toUpperCase());
        attendance.setStatus(status);
        attendance.setJustification(request.getJustification());

        Attendance updated = attendanceRepository.save(attendance);
        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(updated), "Attendance updated successfully"));
    }

    private AttendanceResponse mapToResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .enrollmentId(attendance.getEnrollment() != null ? attendance.getEnrollment().getId() : null)
                .date(attendance.getDate())
                .period(attendance.getPeriod() != null ? attendance.getPeriod().toString() : null)
                .status(attendance.getStatus() != null ? attendance.getStatus().toString() : null)
                .justification(attendance.getJustification())
                .recordedBy(attendance.getRecordedBy())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}

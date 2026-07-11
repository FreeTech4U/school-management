package com.schoolsaas.attendance.controller;

import com.schoolsaas.attendance.dto.request.AttendanceRequest;
import com.schoolsaas.attendance.dto.response.AttendanceResponse;
import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.attendance.mapper.AttendanceMapper;
import com.schoolsaas.attendance.service.AttendanceService;
import com.schoolsaas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/school/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceMapper attendanceMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ApiResponse<AttendanceResponse> recordAttendance(@Valid @RequestBody AttendanceRequest request) {
        Attendance attendance = attendanceMapper.toEntity(request);
        Attendance saved = attendanceService.recordAttendance(attendance);
        return ApiResponse.ok(attendanceMapper.toResponse(saved));
    }
}

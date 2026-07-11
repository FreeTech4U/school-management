package com.schoolsaas.timetable.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.timetable.entity.TimeSlot;
import com.schoolsaas.timetable.entity.TimetableEntry;
import com.schoolsaas.timetable.repository.TimeSlotRepository;
import com.schoolsaas.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;
    private final TimeSlotRepository timeSlotRepository;

    @GetMapping("/time-slots")
    public ApiResponse<List<TimeSlot>> getAllSlots() {
        return ApiResponse.ok(timeSlotRepository.findAll());
    }

    @PostMapping("/timetable")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ApiResponse<TimetableEntry> createEntry(@RequestBody TimetableEntry entry) {
        return ApiResponse.ok(timetableService.createEntry(entry));
    }

    @GetMapping("/timetable/class/{classId}")
    public ApiResponse<List<TimetableEntry>> getByClass(@PathVariable UUID classId, @RequestParam UUID yearId) {
        return ApiResponse.ok(timetableService.getByClass(classId, yearId));
    }
}

package com.schoolsaas.timetable.controller;

import com.schoolsaas.common.dto.ApiResponse;
import com.schoolsaas.common.enums.DayOfWeek;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.timetable.dto.request.TimeSlotRequest;
import com.schoolsaas.timetable.dto.request.TimetableEntryRequest;
import com.schoolsaas.timetable.dto.response.WeeklyTimetableResponse;
import com.schoolsaas.timetable.entity.TimeSlot;
import com.schoolsaas.timetable.entity.TimetableEntry;
import com.schoolsaas.timetable.repository.TimeSlotRepository;
import com.schoolsaas.timetable.repository.TimetableEntryRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for timetable management (emploi du temps) - F-18.
 * Handles time slots and timetable entries for classes and teachers.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/school")
@RequiredArgsConstructor
public class TimetableController {

    private final TimeSlotRepository timeSlotRepository;
    private final TimetableEntryRepository timetableEntryRepository;

    // ============ TimeSlot Endpoints ============

    /**
     * F-18: GET /time-slots - List all time slots
     * Requires: ALL_ROLES
     */
    @GetMapping("/time-slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> listTimeSlots() {
        log.info("Listing all time slots");

        List<TimeSlot> slots = timeSlotRepository.findAll();
        List<TimeSlotResponse> responses = slots.stream()
                .map(this::mapTimeSlotToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses, "Time slots retrieved successfully"));
    }

    /**
     * F-18: POST /time-slots - Create a time slot
     * Requires: DIRECTOR
     */
    @PostMapping("/time-slots")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> createTimeSlot(
            @Valid @RequestBody TimeSlotRequest request) {
        log.info("Creating time slot: {} on {}", request.getLabel(), request.getDayOfWeek());

        TimeSlot slot = TimeSlot.builder()
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .label(request.getLabel())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();

        TimeSlot saved = timeSlotRepository.save(slot);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mapTimeSlotToResponse(saved), "Time slot created successfully"));
    }

    /**
     * F-18: PUT /time-slots/{id} - Update a time slot
     * Requires: DIRECTOR
     */
    @PutMapping("/time-slots/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> updateTimeSlot(
            @PathVariable UUID id, @Valid @RequestBody TimeSlotRequest request) {
        log.info("Updating time slot: {}", id);

        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("TIME_SLOT_NOT_FOUND", "Time slot not found"));

        slot.setLabel(request.getLabel());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setOrderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : slot.getOrderIndex());

        TimeSlot updated = timeSlotRepository.save(slot);
        return ResponseEntity.ok(ApiResponse.ok(mapTimeSlotToResponse(updated), "Time slot updated successfully"));
    }

    /**
     * F-18: DELETE /time-slots/{id} - Delete a time slot
     * Requires: DIRECTOR
     */
    @DeleteMapping("/time-slots/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteTimeSlot(@PathVariable UUID id) {
        log.info("Deleting time slot: {}", id);

        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("TIME_SLOT_NOT_FOUND", "Time slot not found"));

        timeSlotRepository.delete(slot);
        return ResponseEntity.ok(ApiResponse.ok(null, "Time slot deleted successfully"));
    }

    // ============ TimetableEntry Endpoints ============

    /**
     * F-18: GET /timetable/class/{classId} - Get weekly timetable for a class
     * Requires: ALL_ROLES
     */
    @GetMapping("/timetable/class/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WeeklyTimetableResponse>> getClassTimetable(
            @PathVariable UUID classId,
            @RequestParam(required = false) UUID yearId) {
        log.info("Retrieving timetable for class: {}, yearId: {}", classId, yearId);

        // TODO: Build weekly timetable response from timetable entries
        // This requires joining with ClassSubject, TimeSlot, Teacher, Subject
        WeeklyTimetableResponse response = WeeklyTimetableResponse.builder()
                .classId(classId)
                .className("TODO")
                .entries(List.of())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response, "Class timetable retrieved successfully"));
    }

    /**
     * F-18: GET /timetable/teacher/{teacherId} - Get teacher's timetable
     * Requires: DIRECTOR, TEACHER
     */
    @GetMapping("/timetable/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> getTeacherTimetable(
            @PathVariable UUID teacherId,
            @RequestParam(required = false) UUID yearId) {
        log.info("Retrieving timetable for teacher: {}, yearId: {}", teacherId, yearId);

        // TODO: Get all timetable entries where teacher is assigned
        List<TimetableEntryResponse> responses = List.of();

        return ResponseEntity.ok(ApiResponse.ok(responses, "Teacher timetable retrieved successfully"));
    }

    /**
     * F-18: POST /timetable - Create timetable entry
     * Requires: DIRECTOR
     */
    @PostMapping("/timetable")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<TimetableEntryResponse>> createTimetableEntry(
            @Valid @RequestBody TimetableEntryRequest request) {
        log.info("Creating timetable entry for classSubject: {}, timeSlot: {}", 
                request.getClassSubjectId(), request.getTimeSlotId());

        TimetableEntry entry = TimetableEntry.builder()
                .classSubjectId(request.getClassSubjectId())
            //    .timeSlot(request.getTimeSlotId())
                .academicYearId(request.getAcademicYearId())
                .termId(request.getTermId())
                .roomNumber(request.getRoomNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        TimetableEntry saved = timetableEntryRepository.save(entry);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(mapTimetableEntryToResponse(saved), "Timetable entry created successfully"));
    }

    /**
     * F-18: PUT /timetable/{id} - Update timetable entry
     * Requires: DIRECTOR
     */
    @PutMapping("/timetable/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<TimetableEntryResponse>> updateTimetableEntry(
            @PathVariable UUID id, @Valid @RequestBody TimetableEntryRequest request) {
        log.info("Updating timetable entry: {}", id);

        TimetableEntry entry = timetableEntryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("TIMETABLE_ENTRY_NOT_FOUND", "Timetable entry not found"));

        entry.setRoomNumber(request.getRoomNumber());
        entry.setTermId(request.getTermId());
        entry.setIsActive(request.getIsActive() != null ? request.getIsActive() : entry.getIsActive());

        TimetableEntry updated = timetableEntryRepository.save(entry);
        return ResponseEntity.ok(ApiResponse.ok(mapTimetableEntryToResponse(updated), "Timetable entry updated successfully"));
    }

    /**
     * F-18: DELETE /timetable/{id} - Delete timetable entry
     * Requires: DIRECTOR
     */
    @DeleteMapping("/timetable/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteTimetableEntry(@PathVariable UUID id) {
        log.info("Deleting timetable entry: {}", id);

        TimetableEntry entry = timetableEntryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("TIMETABLE_ENTRY_NOT_FOUND", "Timetable entry not found"));

        timetableEntryRepository.delete(entry);
        return ResponseEntity.ok(ApiResponse.ok(null, "Timetable entry deleted successfully"));
    }

    private TimeSlotResponse mapTimeSlotToResponse(TimeSlot slot) {
        return TimeSlotResponse.builder()
                .id(slot.getId())
                .dayOfWeek(slot.getDayOfWeek())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .label(slot.getLabel())
                .orderIndex(slot.getOrderIndex())
                .build();
    }

    private TimetableEntryResponse mapTimetableEntryToResponse(TimetableEntry entry) {
        return TimetableEntryResponse.builder()
                .id(entry.getId())
                .classSubjectId(entry.getClassSubjectId())
                .timeSlotId(entry.getTimeSlot().getId())
                .academicYearId(entry.getAcademicYearId())
                .termId(entry.getTermId())
                .roomNumber(entry.getRoomNumber())
                .isActive(entry.getIsActive())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class TimeSlotResponse {
        private UUID id;
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String label;
        private Short orderIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class TimetableEntryResponse {
        private UUID id;
        private UUID classSubjectId;
        private UUID timeSlotId;
        private UUID academicYearId;
        private UUID termId;
        private String roomNumber;
        private Boolean isActive;
        private Instant createdAt;
    }
}

package com.schoolsaas.timetable.controller;

import com.schoolsaas.common.enums.DayOfWeek;
import com.schoolsaas.support.AbstractControllerTest;
import com.schoolsaas.timetable.dto.request.TimeSlotRequest;
import com.schoolsaas.timetable.dto.request.TimetableEntryRequest;
import com.schoolsaas.timetable.entity.TimeSlot;
import com.schoolsaas.timetable.entity.TimetableEntry;
import com.schoolsaas.timetable.repository.TimeSlotRepository;
import com.schoolsaas.timetable.repository.TimetableEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimetableController.class)
class TimetableControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TimeSlotRepository timeSlotRepository;
    @MockBean
    private TimetableEntryRepository timetableEntryRepository;

    @Test
    @WithMockUser(roles = "TEACHER")
    void listTimeSlots_WithAuthentication_ShouldReturnTimeSlots() throws Exception {
        TimeSlot slot = timeSlot();
        when(timeSlotRepository.findAll()).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/school/time-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("08:00-10:00"))
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createTimeSlot_WithDirectorRole_ShouldCreateTimeSlot() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .label("08:00-10:00")
                .orderIndex((short) 1)
                .build();
        TimeSlot slot = timeSlot();
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(slot);

        mockMvc.perform(post("/api/v1/school/time-slots")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.label").value("08:00-10:00"));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void updateTimeSlot_WithDirectorRole_ShouldUpdateTimeSlot() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .label("10:00-12:00")
                .orderIndex((short) 2)
                .build();
        TimeSlot slot = timeSlot();
        when(timeSlotRepository.findById(slot.getId())).thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/school/time-slots/{id}", slot.getId())
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("10:00-12:00"))
                .andExpect(jsonPath("$.data.orderIndex").value(2));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void deleteTimeSlot_WithDirectorRole_ShouldDeleteTimeSlot() throws Exception {
        TimeSlot slot = timeSlot();
        when(timeSlotRepository.findById(slot.getId())).thenReturn(Optional.of(slot));

        mockMvc.perform(delete("/api/v1/school/time-slots/{id}", slot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Time slot deleted successfully"));

        verify(timeSlotRepository).delete(slot);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getClassTimetable_WithAuthentication_ShouldReturnPlaceholderResponse() throws Exception {
        UUID classId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/school/timetable/class/{classId}", classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(classId.toString()))
                .andExpect(jsonPath("$.data.className").value("TODO"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getTeacherTimetable_WithTeacherRole_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/school/timetable/teacher/{teacherId}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void createTimetableEntry_WithDirectorRole_ShouldCreateEntry() throws Exception {
        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .classSubjectId(UUID.randomUUID())
                .timeSlotId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .termId(UUID.randomUUID())
                .roomNumber("B12")
                .isActive(true)
                .build();
        TimetableEntry entry = timetableEntry(request);
        when(timetableEntryRepository.save(any(TimetableEntry.class))).thenReturn(entry);

        mockMvc.perform(post("/api/v1/school/timetable")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roomNumber").value("B12"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void updateTimetableEntry_WithDirectorRole_ShouldUpdateEntry() throws Exception {
        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .classSubjectId(UUID.randomUUID())
                .timeSlotId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .termId(UUID.randomUUID())
                .roomNumber("C21")
                .isActive(false)
                .build();
        TimetableEntry entry = timetableEntry(request);
        when(timetableEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(timetableEntryRepository.save(any(TimetableEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/school/timetable/{id}", entry.getId())
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomNumber").value("C21"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void createTimetableEntry_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .classSubjectId(UUID.randomUUID())
                .timeSlotId(UUID.randomUUID())
                .academicYearId(UUID.randomUUID())
                .roomNumber("B12")
                .build();

        mockMvc.perform(post("/api/v1/school/timetable")
                        .contentType("application/json")
                        .content(asJson(request)))
                .andExpect(status().isUnauthorized());
    }

    private TimeSlot timeSlot() {
        TimeSlot slot = TimeSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .label("08:00-10:00")
                .orderIndex((short) 1)
                .build();
        slot.setId(UUID.randomUUID());
        return slot;
    }

    private TimetableEntry timetableEntry(TimetableEntryRequest request) {
        TimeSlot slot = timeSlot();
        slot.setId(request.getTimeSlotId());
        TimetableEntry entry = TimetableEntry.builder()
                .classSubjectId(request.getClassSubjectId())
                .timeSlot(slot)
                .academicYearId(request.getAcademicYearId())
                .termId(request.getTermId())
                .roomNumber(request.getRoomNumber())
                .isActive(request.getIsActive())
                .build();
        entry.setId(UUID.randomUUID());
        entry.setCreatedAt(Instant.now());
        return entry;
    }
}

package com.schoolsaas.timetable.service;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.repository.ClassSubjectRepository;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.timetable.entity.TimetableEntry;
import com.schoolsaas.timetable.repository.TimetableEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TimetableEntryRepository timetableEntryRepository;
    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @InjectMocks
    private TimetableService timetableService;

    @Test
    void createEntry_Success() {
        // Given
        UUID csId = UUID.randomUUID();
        UUID tsId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        TimetableEntry entry = new TimetableEntry();
        entry.setClassSubjectId(csId);
        entry.setTimeSlotId(tsId);
        entry.setAcademicYearId(yearId);

        ClassSubject cs = new ClassSubject();
        cs.setId(csId);
        cs.setClassId(classId);
        cs.setTeacherId(teacherId);

        when(classSubjectRepository.findById(csId)).thenReturn(Optional.of(cs));
        when(timetableEntryRepository.existsConflictForClass(classId, tsId, yearId)).thenReturn(false);
        when(timetableEntryRepository.existsConflictForTeacher(teacherId, tsId, yearId)).thenReturn(false);
        when(timetableEntryRepository.save(any(TimetableEntry.class))).thenReturn(entry);

        // When
        TimetableEntry result = timetableService.createEntry(entry);

        // Then
        assertNotNull(result);
        verify(timetableEntryRepository).save(entry);
    }

    @Test
    void createEntry_ClassConflict_ThrowsException() {
        // Given
        UUID csId = UUID.randomUUID();
        TimetableEntry entry = new TimetableEntry();
        entry.setClassSubjectId(csId);

        ClassSubject cs = new ClassSubject();
        cs.setId(csId);
        cs.setClassId(UUID.randomUUID());

        when(classSubjectRepository.findById(csId)).thenReturn(Optional.of(cs));
        when(timetableEntryRepository.existsConflictForClass(any(), any(), any())).thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> timetableService.createEntry(entry));
    }
}

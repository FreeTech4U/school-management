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
    void createEntry_ClassConflict_ThrowsException() {
        // Given
        UUID csId = UUID.randomUUID();
        UUID tsId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        
        TimetableEntry entry = new TimetableEntry();
        entry.setClassSubjectId(csId);
        entry.setTimeSlotId(tsId);
        entry.setAcademicYearId(yearId);

        ClassSubject cs = new ClassSubject();
        cs.setId(csId);

        when(classSubjectRepository.findById(csId)).thenReturn(Optional.of(cs));
        when(timetableEntryRepository.existsConflictForClass(any(), any(), any())).thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> timetableService.createEntry(entry));
        verify(timetableEntryRepository, never()).save(any());
    }

    @Test
    void createEntry_ClassSubjectNotFound_ThrowsException() {
        // Given
        UUID csId = UUID.randomUUID();
        
        TimetableEntry entry = new TimetableEntry();
        entry.setClassSubjectId(csId);

        when(classSubjectRepository.findById(csId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BusinessException.class, () -> timetableService.createEntry(entry));
    }
}

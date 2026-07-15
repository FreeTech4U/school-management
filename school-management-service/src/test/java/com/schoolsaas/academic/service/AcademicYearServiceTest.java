package com.schoolsaas.academic.service;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.repository.AcademicYearRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.enums.YearStatus;
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
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private TermRepository termRepository;

    @InjectMocks
    private AcademicYearService academicYearService;

    @Test
    void createYear_SetsCurrent_ResetsOthers() {
        // Given
        AcademicYear year = new AcademicYear();
        year.setIsCurrent(true);
        year.setLabel("2024-2025");

        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(year);

        // When
        academicYearService.createYear(year);

        // Then
        verify(academicYearRepository).resetCurrentYear();
        verify(academicYearRepository).save(year);
    }

    @Test
    void closeYear_Success() {
        // Given
        UUID id = UUID.randomUUID();
        AcademicYear year = new AcademicYear();
        year.setId(id);
        year.setStatus(YearStatus.ACTIVE);
        year.setIsCurrent(true);

        when(academicYearRepository.findById(id)).thenReturn(Optional.of(year));

        // When
        academicYearService.closeYear(id);

        // Then
        assertEquals(YearStatus.CLOSED, year.getStatus());
        assertFalse(year.getIsCurrent());
        verify(academicYearRepository).save(year);
    }

    @Test
    void setGradesEntryStatus_Success() {
        // Given
        UUID termId = UUID.randomUUID();
        Term term = new Term();
        term.setId(termId);
        term.setGradesEntryOpen(false);

        when(termRepository.findById(termId)).thenReturn(Optional.of(term));

        // When
        academicYearService.setGradesEntryStatus(termId, true);

        // Then
        assertTrue(term.getGradesEntryOpen());
        verify(termRepository).save(term);
    }
}

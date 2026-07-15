package com.schoolsaas.grading.service;

import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.repository.ClassSubjectRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.repository.GradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @InjectMocks
    private GradeService gradeService;

    @Test
    void enterGrade_WithClosedTerm_ShouldThrowException() {
        // Given
        UUID termId = UUID.randomUUID();
        Term term = new Term();
        term.setGradesEntryOpen(false);
        term.setId(termId);

        Grade grade = Grade.builder()
                .termId(termId)
                .classSubjectId(UUID.randomUUID())
                .build();
        when(termRepository.findById(termId)).thenReturn(Optional.of(term));

        // When & Then
        assertThrows(BusinessException.class, () -> gradeService.enterGrade(grade));
        verify(gradeRepository, never()).save(any());
    }
}

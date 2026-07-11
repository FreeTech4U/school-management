package com.schoolsaas.grading.service;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.repository.ClassSubjectRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.config.security.AuthenticatedUser;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.repository.GradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void enterGrade_Success() {
        // Given
        UUID termId = UUID.randomUUID();
        UUID classSubjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        
        Grade grade = new Grade();
        grade.setTermId(termId);
        grade.setClassSubjectId(classSubjectId);
        grade.setValue(BigDecimal.valueOf(15.5));

        Term term = new Term();
        term.setId(termId);
        term.setGradesEntryOpen(true);

        ClassSubject classSubject = new ClassSubject();
        classSubject.setId(classSubjectId);
        classSubject.setTeacherId(teacherId);

        AuthenticatedUser user = AuthenticatedUser.builder()
                .userId(teacherId)
                .roles(List.of("TEACHER"))
                .build();
        
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(classSubjectRepository.findById(classSubjectId)).thenReturn(Optional.of(classSubject));
        when(gradeRepository.save(any(Grade.class))).thenReturn(grade);

        // When
        Grade result = gradeService.enterGrade(grade);

        // Then
        assertNotNull(result);
        verify(gradeRepository).save(grade);
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void enterGrade_EntryClosed_ThrowsException() {
        // Given
        UUID termId = UUID.randomUUID();
        Grade grade = new Grade();
        grade.setTermId(termId);

        Term term = new Term();
        term.setId(termId);
        term.setGradesEntryOpen(false);

        when(termRepository.findById(termId)).thenReturn(Optional.of(term));

        // When & Then
        assertThrows(BusinessException.class, () -> gradeService.enterGrade(grade));
    }

    @Test
    void enterGrade_WrongTeacher_ThrowsException() {
        // Given
        UUID termId = UUID.randomUUID();
        UUID classSubjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID otherTeacherId = UUID.randomUUID();
        
        Grade grade = new Grade();
        grade.setTermId(termId);
        grade.setClassSubjectId(classSubjectId);

        Term term = new Term();
        term.setId(termId);
        term.setGradesEntryOpen(true);

        ClassSubject classSubject = new ClassSubject();
        classSubject.setId(classSubjectId);
        classSubject.setTeacherId(teacherId);

        AuthenticatedUser user = AuthenticatedUser.builder()
                .userId(otherTeacherId)
                .roles(List.of("TEACHER"))
                .build();
        
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        when(termRepository.findById(termId)).thenReturn(Optional.of(term));
        when(classSubjectRepository.findById(classSubjectId)).thenReturn(Optional.of(classSubject));

        // When & Then
        assertThrows(BusinessException.class, () -> gradeService.enterGrade(grade));
        
        SecurityContextHolder.clearContext();
    }
}

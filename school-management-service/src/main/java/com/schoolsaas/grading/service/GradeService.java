package com.schoolsaas.grading.service;

import com.schoolsaas.academic.entity.ClassSubject;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.repository.ClassSubjectRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.config.security.AuthenticatedUser;
import com.schoolsaas.grading.entity.Grade;
import com.schoolsaas.grading.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final TermRepository termRepository;
    private final ClassSubjectRepository classSubjectRepository;

    @Transactional
    public Grade enterGrade(Grade grade) {
        Term term = grade.getTerm();
        if (term == null) {
            term = termRepository.findById(grade.getTerm().getId())
                    .orElseThrow(() -> BusinessException.notFound("TERM_NOT_FOUND", "Trimestre introuvable"));
        }

        if (!Boolean.TRUE.equals(term.getGradesEntryOpen())) {
            throw new BusinessException("GRADES_ENTRY_CLOSED", "La saisie des notes est fermée pour ce trimestre");
        }

        // Logic to verify teacher assignment
        verifyTeacherAssignment(grade.getClassSubject().getId());

        return gradeRepository.save(grade);
    }

    private void verifyTeacherAssignment(UUID classSubjectId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            if (user.getRoles().contains("TEACHER")) {
                ClassSubject cs = classSubjectRepository.findById(classSubjectId)
                        .orElseThrow(() -> BusinessException.notFound("CLASS_SUBJECT_NOT_FOUND", "Matière de classe introuvable"));
                if (!user.getUserId().equals(cs.getTeacher().getId())) {
                    throw BusinessException.forbidden("UNAUTHORIZED_SUBJECT", "Vous n'êtes pas affecté à cette matière");
                }
            }
        }
    }

    public List<Grade> getGradesForClassSubject(UUID classSubjectId, UUID termId) {
        return gradeRepository.findByClassSubjectIdAndTermId(classSubjectId, termId);
    }
}

package com.schoolsaas.academic.service;

import com.schoolsaas.academic.entity.AcademicYear;
import com.schoolsaas.academic.entity.Term;
import com.schoolsaas.academic.repository.AcademicYearRepository;
import com.schoolsaas.academic.repository.TermRepository;
import com.schoolsaas.common.enums.YearStatus;
import com.schoolsaas.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;

    public List<AcademicYear> getAllYears() {
        return academicYearRepository.findAll();
    }

    public AcademicYear getYearById(UUID id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ACADEMIC_YEAR_NOT_FOUND", "Année scolaire introuvable"));
    }

    public AcademicYear getCurrentYear() {
        return academicYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> BusinessException.notFound("NO_CURRENT_YEAR", "Aucune année scolaire active"));
    }

    @Transactional
    public AcademicYear createYear(AcademicYear year) {
        if (year.getIsCurrent()) {
            academicYearRepository.resetCurrentYear();
        }
        return academicYearRepository.save(year);
    }

    @Transactional
    public AcademicYear updateYear(UUID id, AcademicYear yearDetails) {
        AcademicYear year = getYearById(id);
        if (YearStatus.CLOSED.equals(year.getStatus())) {
            throw new BusinessException("YEAR_CLOSED", "Une année fermée ne peut plus être modifiée");
        }
        
        year.setLabel(yearDetails.getLabel());
        year.setStartDate(yearDetails.getStartDate());
        year.setEndDate(yearDetails.getEndDate());
        
        if (yearDetails.getIsCurrent() && !year.getIsCurrent()) {
            academicYearRepository.resetCurrentYear();
            year.setIsCurrent(true);
        }
        
        return academicYearRepository.save(year);
    }

    @Transactional
    public void closeYear(UUID id) {
        AcademicYear year = getYearById(id);
        year.setStatus(YearStatus.CLOSED);
        year.setIsCurrent(false);
        academicYearRepository.save(year);
        // Trigger promotions (Phase 2)
    }

    // Term Methods
    public List<Term> getTermsByYear(UUID yearId) {
        return termRepository.findByAcademicYearId(yearId);
    }

    @Transactional
    public Term createTerm(UUID yearId, Term term) {
        AcademicYear year = getYearById(yearId);
        term.setAcademicYear(year);
        if (term.getIsCurrent()) {
            termRepository.resetCurrentTermForYear(yearId);
        }
        return termRepository.save(term);
    }

    @Transactional
    public void setGradesEntryStatus(UUID termId, boolean open) {
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> BusinessException.notFound("TERM_NOT_FOUND", "Trimestre introuvable"));
        term.setGradesEntryOpen(open);
        termRepository.save(term);
    }
}

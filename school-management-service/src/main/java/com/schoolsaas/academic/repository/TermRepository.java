package com.schoolsaas.academic.repository;

import com.schoolsaas.academic.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TermRepository extends JpaRepository<Term, UUID> {
    List<Term> findByAcademicYearId(UUID academicYearId);

    @Modifying
    @Query("UPDATE Term t SET t.isCurrent = false WHERE t.academicYear.id = :yearId AND t.isCurrent = true")
    void resetCurrentTermForYear(UUID yearId);
}

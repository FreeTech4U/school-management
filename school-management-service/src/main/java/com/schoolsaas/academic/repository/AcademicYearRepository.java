package com.schoolsaas.academic.repository;

import com.schoolsaas.academic.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    Optional<AcademicYear> findByIsCurrentTrue();

    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.isCurrent = true")
    void resetCurrentYear();
}

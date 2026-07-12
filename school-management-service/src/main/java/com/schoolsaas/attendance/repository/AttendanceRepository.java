package com.schoolsaas.attendance.repository;

import com.schoolsaas.attendance.entity.Attendance;
import com.schoolsaas.common.enums.Period;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByEnrollmentIdAndDate(UUID enrollmentId, LocalDate date);
    boolean existsByEnrollmentIdAndDateAndPeriod(UUID enrollmentId, LocalDate date, Period period);
}

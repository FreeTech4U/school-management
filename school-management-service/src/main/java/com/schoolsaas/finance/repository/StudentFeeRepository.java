package com.schoolsaas.finance.repository;

import com.schoolsaas.finance.entity.StudentFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentFeeRepository extends JpaRepository<StudentFee, UUID> {
    List<StudentFee> findByEnrollmentId(UUID enrollmentId);
    List<StudentFee> findByStatusIn(List<String> statuses);
}

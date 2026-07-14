package com.schoolsaas.finance.repository;

import com.schoolsaas.finance.entity.StudentFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StudentFeeRepository extends JpaRepository<StudentFee, UUID> {
    List<StudentFee> findByEnrollmentId(UUID enrollmentId);
    List<StudentFee> findByStatusIn(List<String> statuses);

    @Modifying
    @Query("""
    UPDATE StudentFee sf
    SET    sf.status = com.schoolsaas.common.enums.FeeStatus.OVERDUE
    WHERE  sf.status IN (com.schoolsaas.common.enums.FeeStatus.UNPAID,
                          com.schoolsaas.common.enums.FeeStatus.PARTIAL)
      AND  sf.dueDate IS NOT NULL
      AND  sf.dueDate < CURRENT_DATE
    """)
    int markOverdueFees();


    @Query("""
    SELECT sf FROM StudentFee sf
    JOIN FETCH sf.feeStructure fs
    WHERE sf.status IN (com.schoolsaas.common.enums.FeeStatus.UNPAID,
                        com.schoolsaas.common.enums.FeeStatus.PARTIAL,
                        com.schoolsaas.common.enums.FeeStatus.OVERDUE)
      AND (sf.lastReminderSentAt IS NULL OR sf.lastReminderSentAt < :cutoff)
    ORDER BY sf.dueDate ASC NULLS LAST
    """)
    List<StudentFee> findFeesNeedingReminder(@Param("cutoff") Instant cutoff);
}

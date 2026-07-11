package com.schoolsaas.communication.repository;

import com.schoolsaas.communication.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SmsLogRepository extends JpaRepository<SmsLog, UUID> {
    List<SmsLog> findByStudentId(UUID studentId);
}

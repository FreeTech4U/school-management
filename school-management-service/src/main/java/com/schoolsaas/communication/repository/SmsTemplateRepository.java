package com.schoolsaas.communication.repository;

import com.schoolsaas.communication.entity.SmsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, UUID> {
    Optional<SmsTemplate> findByCode(String code);
}

package com.schoolsaas.communication.scheduler;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.communication.service.FeeReminderService;
import com.schoolsaas.communication.service.SmsService;
import com.schoolsaas.config.multitenancy.TenantContext;
import com.schoolsaas.finance.entity.StudentFee;
import com.schoolsaas.finance.repository.StudentFeeRepository;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsScheduler {

    private final SchoolRepository schoolRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final FeeReminderService feeReminderService;


    @Scheduled(
            cron = "${app.scheduler.fee-reminder.cron}",
            zone = "Africa/Conakry" // A revoir
    )
    public void sendFeeReminders() {
        List<School> schools = schoolRepository.findAllByStatusIn(List.of(SchoolStatus.TRIAL, SchoolStatus.ACTIVE));
        log.info("Rappels de frais — {} école(s)", schools.size());

        for (School school : schools) {
            try {
                TenantContext.set(school.getSchemaName());

                // Appel EXTERNE → passe par le proxy Spring → @Transactional actif
                int sent = feeReminderService.processSchoolReminders();

                log.info("{} rappel(s) envoyé(s) pour {}", sent, school.getName());

            } catch (Exception e) {
                log.error("Échec des rappels pour {} (schema={}): {}",
                        school.getName(), school.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Scheduled(cron = "${app.scheduler.overdue-fees.cron}", zone = "Africa/Conakry")
    @Transactional
    public void markOverdueFees() {
        for (School school : schoolRepository.findAllByStatusIn(List.of(SchoolStatus.TRIAL, SchoolStatus.ACTIVE))) {
            try {
                TenantContext.set(school.getSchemaName());
                int updated = studentFeeRepository.markOverdueFees();
                log.info("{} frais passés en OVERDUE pour {}", updated, school.getName());
            } catch (Exception e) {
                log.error("Échec markOverdueFees pour {}: {}", school.getSchemaName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}

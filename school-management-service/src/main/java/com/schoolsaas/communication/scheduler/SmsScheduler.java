package com.schoolsaas.communication.scheduler;

import com.schoolsaas.communication.service.SmsService;
import com.schoolsaas.config.multitenancy.TenantContext;
import com.schoolsaas.finance.entity.StudentFee;
import com.schoolsaas.finance.repository.StudentFeeRepository;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SmsService smsService;

    @Scheduled(cron = "0 0 8 * * MON-FRI") // Monday to Friday at 8 AM
    public void sendBulkFeeReminders() {
        log.info("Starting scheduled bulk fee reminders...");
        List<School> schools = schoolRepository.findAll();
        
        for (School school : schools) {
            if (!"active".equals(school.getStatus()) && !"trial".equals(school.getStatus())) continue;
            
            try {
                TenantContext.set(school.getSchemaName());
                processSchoolReminders(school);
            } catch (Exception e) {
                log.error("Error processing reminders for school {}: {}", school.getName(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Transactional
    public void processSchoolReminders(School school) {
        // Find overdue or unpaid fees that haven't had a reminder in 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<StudentFee> fees = studentFeeRepository.findByStatusIn(List.of("UNPAID", "PARTIAL", "OVERDUE"));
        
        for (StudentFee fee : fees) {
            if (fee.getLastReminderSentAt() != null && fee.getLastReminderSentAt().isAfter(sevenDaysAgo)) continue;
            
            // Note: We'd need to fetch Student info from enrollment domain here
            // For brevity, assuming we have access or use a simplified approach
            // In a real app, we would call StudentService.getStudentInfo(fee.getEnrollmentId())
            
            // Update last reminder date
            fee.setLastReminderSentAt(LocalDateTime.now());
            studentFeeRepository.save(fee);
            
            // Trigger SMS (SmsService is @Async)
            // smsService.sendTemplatedSms(...)
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // Every day at midnight
    public void markOverdueFees() {
        log.info("Starting scheduled marking of overdue fees...");
        List<School> schools = schoolRepository.findAll();
        for (School school : schools) {
            try {
                TenantContext.set(school.getSchemaName());
                // Logic to update status='OVERDUE' where status IN ('UNPAID','PARTIAL') and due_date < today
            } finally {
                TenantContext.clear();
            }
        }
    }
}

package com.schoolsaas.communication.service;

import com.schoolsaas.common.enums.SmsStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.communication.entity.SmsLog;
import com.schoolsaas.communication.entity.SmsTemplate;
import com.schoolsaas.communication.repository.SmsLogRepository;
import com.schoolsaas.communication.repository.SmsTemplateRepository;
import com.schoolsaas.infrastructure.sms.SmsProvider;
import com.schoolsaas.infrastructure.sms.SmsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsTemplateRepository templateRepository;
    private final SmsLogRepository smsLogRepository;
    private final SmsProvider smsProvider;
    private final SmsTemplateEngine templateEngine;

    @Async
    @Transactional
    public void sendTemplatedSms(UUID studentId, String phone, String templateCode, Map<String, String> variables) {
        try {
            SmsTemplate template = templateRepository.findByCode(templateCode)
                    .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "Template SMS introuvable: " + templateCode));

            String message = templateEngine.resolve(template.getContentFr(), variables);
            
            SmsLog smsLog = SmsLog.builder()
                    .studentId(studentId)
                    .recipientPhone(phone)
                    .message(message)
                    .provider(smsProvider.getProviderName())
                    .status(SmsStatus.PENDING)
                    .build();
            smsLog = smsLogRepository.save(smsLog);

            SmsResult result = smsProvider.send(phone, message);

            smsLog.setProviderMessageId(result.getProviderMessageId());
            if (result.isSuccess()) {
                smsLog.setStatus(SmsStatus.SENT);
                smsLog.setSentAt(LocalDateTime.now());
            } else {
                smsLog.setStatus(SmsStatus.FAILED);
                smsLog.setErrorCode(result.getErrorCode());
                smsLog.setErrorMessage(result.getErrorMessage());
            }
            smsLogRepository.save(smsLog);

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phone, e.getMessage());
            // No exception thrown to avoid breaking caller transaction (requirement 5)
        }
    }
}

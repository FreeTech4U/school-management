package com.schoolsaas.communication.service;

import com.schoolsaas.communication.entity.SmsLog;
import com.schoolsaas.communication.entity.SmsTemplate;
import com.schoolsaas.communication.repository.SmsLogRepository;
import com.schoolsaas.communication.repository.SmsTemplateRepository;
import com.schoolsaas.infrastructure.sms.SmsProvider;
import com.schoolsaas.infrastructure.sms.SmsResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private SmsTemplateRepository templateRepository;
    @Mock
    private SmsLogRepository smsLogRepository;
    @Mock
    private SmsProvider smsProvider;
    @Mock
    private SmsTemplateEngine templateEngine;

    @InjectMocks
    private SmsService smsService;

    @Test
    void sendTemplatedSms_Success() {
        // Given
        UUID studentId = UUID.randomUUID();
        String phone = "+224622112233";
        String templateCode = "test_code";
        Map<String, String> vars = Map.of("name", "John");
        
        SmsTemplate template = new SmsTemplate();
        template.setContentFr("Hello {{name}}");
        
        when(templateRepository.findByCode(templateCode)).thenReturn(Optional.of(template));
        when(templateEngine.resolve(anyString(), any())).thenReturn("Hello John");
        when(smsLogRepository.save(any(SmsLog.class))).thenAnswer(i -> i.getArgument(0));
        when(smsProvider.getProviderName()).thenReturn("test-provider");
        when(smsProvider.send(anyString(), anyString())).thenReturn(SmsResult.builder().success(true).build());

        // When
        smsService.sendTemplatedSms(studentId, phone, templateCode, vars);

        // Then
        verify(smsProvider).send(phone, "Hello John");
        verify(smsLogRepository, times(2)).save(any(SmsLog.class));
    }
}

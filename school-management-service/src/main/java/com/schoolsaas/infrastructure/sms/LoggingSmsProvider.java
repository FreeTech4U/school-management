package com.schoolsaas.infrastructure.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LoggingSmsProvider implements SmsProvider {

    @Override
    public SmsResult send(String to, String message) {
        log.info("[SMS-LOG] To: {}, Message: {}", to, message);
        return SmsResult.builder()
                .success(true)
                .providerMessageId(UUID.randomUUID().toString())
                .recipientPhone(to)
                .build();
    }

    @Override
    public List<SmsResult> sendBulk(List<String> recipients, String message) {
        return recipients.stream()
                .map(r -> send(r, message))
                .collect(Collectors.toList());
    }

    @Override
    public String getProviderName() {
        return "logging";
    }
}

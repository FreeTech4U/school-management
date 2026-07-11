package com.schoolsaas.infrastructure.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SmsProviderConfig {

    @Value("${app.sms.provider:logging}")
    private String providerName;

    private final List<SmsProvider> providers;

    @Bean
    @Primary
    public SmsProvider activeSmsProvider() {
        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElse(providers.stream()
                        .filter(p -> p.getProviderName().equalsIgnoreCase("logging"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No SMS Provider found")));
    }
}

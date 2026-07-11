package com.schoolsaas.infrastructure.sms;

import java.util.List;

public interface SmsProvider {
    SmsResult send(String to, String message);
    List<SmsResult> sendBulk(List<String> recipients, String message);
    String getProviderName();
}

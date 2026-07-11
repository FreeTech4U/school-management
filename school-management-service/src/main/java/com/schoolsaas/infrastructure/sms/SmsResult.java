package com.schoolsaas.infrastructure.sms;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SmsResult {
    private boolean success;
    private String providerMessageId;
    private String errorCode;
    private String errorMessage;
    private String recipientPhone;
}

package com.schoolsaas.communication.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.SmsStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sms_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsLog extends BaseEntity {

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String message;

    private String provider;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SmsStatus status = SmsStatus.PENDING;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}

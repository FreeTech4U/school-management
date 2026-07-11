package com.schoolsaas.communication.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sms_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplate extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String category; // FINANCIAL, ACADEMIC

    @Column(name = "content_fr", nullable = false)
    private String contentFr;

    @Column(name = "variables", columnDefinition = "JSONB")
    private String variables;
}

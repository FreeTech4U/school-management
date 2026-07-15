package com.schoolsaas.communication.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.SmsCategory;
import com.schoolsaas.common.enums.SmsStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Journal de TOUS les SMS, échecs compris (F-13).
 *
 * Sert au suivi des relances, au débogage et au décompte du quota mensuel
 * (school_subscriptions.sms_used_this_month).
 *
 * CORRECTIONS :
 *   • AJOUT du champ template : la colonne template_id existait en base mais pas
 *     dans l'entité. Sans elle, impossible de savoir quel modèle avait produit
 *     un message donné.
 *     C'est une association INTRA-domaine (communication → communication) :
 *     la @ManyToOne est légitime.
 *
 *   • sentAt et deliveredAt passent de LocalDateTime à Instant
 *     (colonnes TIMESTAMPTZ).
 *
 *   • category devient nullable, comme en base : un SMS libre (CUSTOM) envoyé
 *     hors modèle peut ne pas porter de catégorie.
 */
@Entity
@Table(name = "sms_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsLog extends BaseEntity {

    /** INTRA-domaine : @ManyToOne. NULL si SMS envoyé hors modèle. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private SmsTemplate template;

    /** INTER-domaine (communication → enrollment) : UUID. */
    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    /** Contenu APRÈS résolution des variables. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Nullable : un SMS libre peut ne pas avoir de catégorie. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SmsCategory category;

    /** orange · twilio · logging */
    @Column(length = 50)
    private String provider;

    /** Identifiant retourné par l'opérateur, sert au suivi de livraison. */
    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    /** PENDING (défaut) · SENT · DELIVERED · FAILED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SmsStatus status = SmsStatus.PENDING;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** CORRECTION : Instant (colonnes TIMESTAMPTZ). */
    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;
}

package com.schoolsaas.finance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.PaymentMethod;
import com.schoolsaas.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Paiement encaissé par l'école (F-12).
 *
 * NUMÉRO DE REÇU : receiptNumber est généré par un trigger PostgreSQL au format
 * REC-YYYYMM-NNNN. Ne jamais le renseigner depuis le code.
 *
 * ANNULATION : un paiement n'est JAMAIS supprimé. cancelPayment() passe le
 * statut à CANCELLED et renseigne les trois colonnes de traçabilité ci-dessous.
 * Un trigger (fn_payment_status_changed) recalcule alors automatiquement le
 * statut de tous les frais qui étaient imputés par ce paiement.
 *
 * CORRECTIONS :
 *   • AJOUT de cancellationReason / cancelledBy / cancelledAt.
 *     Ces trois colonnes existaient en base, avec une contrainte CHECK qui les
 *     EXIGE quand le statut vaut CANCELLED. Sans elles, la première annulation
 *     violait la contrainte.
 *   • paymentDate passe de LocalDateTime à LocalDate : la colonne SQL est de
 *     type DATE. Un paiement a une date, pas une heure — cela évite les
 *     décalages de fuseau dans les rapports journaliers.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    /** INTER-domaine (finance → enrollment) : UUID. */
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /** REC-YYYYMM-NNNN. Généré par le trigger trg_generate_receipt_number. */
    @Column(name = "receipt_number", unique = true, length = 30)
    private String receiptNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** CORRECTION : LocalDate (colonne DATE), et non LocalDateTime. */
    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private LocalDate paymentDate = LocalDate.now();

    /** CASH · ORANGE_MONEY · MTN_MONEY · WAVE · BANK_TRANSFER · CHECK */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** CONFIRMED (défaut) · CANCELLED · REFUNDED. Seul CONFIRMED compte au solde. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.CONFIRMED;

    /** Réf. Orange Money, n° de virement, n° de chèque. */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Comptable ayant enregistré le paiement. INTER-domaine : UUID. */
    @Column(name = "recorded_by")
    private UUID recordedBy;

    /** AJOUT — traçabilité de l'annulation (exigée par un CHECK en base). */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * INTRA-domaine : les imputations appartiennent au paiement (composition).
     * Le cascade est légitime ici : supprimer un paiement supprime ses
     * imputations. Mais on ne supprime jamais un paiement — on l'annule.
     */
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentAllocation> allocations = new ArrayList<>();
}


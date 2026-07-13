package com.schoolsaas.finance.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.FeeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Dette individuelle d'un élève sur un frais donné (F-11).
 *
 * GÉNÉRATION AUTOMATIQUE : ces lignes sont créées à l'inscription.
 * EnrollmentService.enroll() appelle StudentFeeService.generateFeesForEnrollment(),
 * qui crée un StudentFee par FeeStructure applicable (F-09).
 *
 * ⚠ amountPaid et status sont MAINTENUS PAR UN TRIGGER PostgreSQL
 *   (fn_recalculate_fee_status). Le code Java ne doit JAMAIS les écrire, à
 *   l'unique exception du statut WAIVED (exonération décidée par le directeur —
 *   le trigger est programmé pour ne jamais l'écraser).
 *   Après un save() de Payment, penser à un flush()+refresh() si la valeur
 *   recalculée est nécessaire immédiatement.
 *
 * CORRECTIONS :
 *   • enrollment passe d'une @ManyToOne<StudentEnrollment> à un UUID :
 *     StudentEnrollment appartient au domaine enrollment/, l'association
 *     violait la règle inter-domaines.
 *     → conséquence : la requête findFeesNeedingReminder() ne peut PAS faire
 *       « JOIN FETCH sf.enrollment ». Le service récupère les coordonnées des
 *       parents via EnrollmentService.getContactInfoByEnrollmentIds(), en un
 *       seul appel (anti N+1).
 *   • La collection List<PaymentAllocation> est supprimée : une allocation
 *     appartient au Payment, pas au StudentFee. La conserver ici, avec
 *     cascade = ALL, signifiait que supprimer un frais effaçait des imputations
 *     de paiement — corruption comptable.
 *   • dueDate devient nullable (aligné sur la colonne SQL).
 */
@Entity
@Table(
        name = "student_fees",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_student_fee",
                columnNames = {"enrollment_id", "fee_structure_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFee extends BaseEntity {

    /** INTER-domaine (finance → enrollment) : UUID. */
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    /** INTRA-domaine : @ManyToOne. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_structure_id", nullable = false)
    private FeeStructure feeStructure;

    @Column(name = "amount_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountDue;

    /** ⚠ Calculé par trigger. Ne pas écrire depuis le code. */
    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    /** Remise : bourse, fratrie, cas social. Saisie manuelle par le directeur. */
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    /** Hérité de FeeStructure. Nullable. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** ⚠ Calculé par trigger, sauf WAIVED. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FeeStatus status = FeeStatus.UNPAID;

    /**
     * Anti-spam des rappels SMS : un parent reçoit au maximum un rappel par
     * semaine et par frais (F-13).
     * Instant, et non LocalDateTime : la colonne est TIMESTAMPTZ.
     */
    @Column(name = "last_reminder_sent_at")
    private Instant lastReminderSentAt;

    /** Reste dû, remise déduite. Calcul en mémoire, non persisté. */
    @Transient
    public BigDecimal getRemainingAmount() {
        return amountDue.subtract(discountAmount).subtract(amountPaid);
    }
}

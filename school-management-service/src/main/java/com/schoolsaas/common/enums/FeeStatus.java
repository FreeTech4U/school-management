package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'un frais de scolarité pour un élève donné (F-11).
 *
 * Ce statut est calculé AUTOMATIQUEMENT par le trigger PostgreSQL
 * fn_recalculate_fee_status(), déclenché après chaque INSERT/UPDATE/DELETE
 * sur payment_allocations. Il ne doit jamais être positionné à la main
 * depuis le code Java — sauf WAIVED, seule décision manuelle.
 *
 * RÈGLE DE CALCUL :
 *   restant = amount_due - discount_amount - amount_paid
 *   restant <= 0                          → PAID
 *   amount_paid > 0                       → PARTIAL
 *   due_date dépassée                     → OVERDUE  (via scheduler quotidien)
 *   sinon                                 → UNPAID
 *
 * CORRECTION vs version précédente :
 *   - EXEMPTED → supprimé. C'était un doublon conceptuel de WAIVED, et surtout
 *                un piège : le trigger fn_recalculate_fee_status ne protège que
 *                le statut WAIVED. Un frais EXEMPTED aurait été silencieusement
 *                réécrit en UNPAID au premier paiement d'un autre frais.
 *
 * Type PostgreSQL correspondant : fee_status
 */
@Getter
@RequiredArgsConstructor
public enum FeeStatus {

    /** Aucun paiement reçu, échéance non dépassée. Statut par défaut. */
    UNPAID  ("Non payé"),

    /** Paiement partiel : un solde reste dû. */
    PARTIAL ("Partiellement payé"),

    /** Frais entièrement soldé (remise incluse). */
    PAID    ("Payé"),

    /** Échéance dépassée et solde non nul. Positionné par le scheduler à minuit. */
    OVERDUE ("En retard"),

    /**
     * Frais exonéré par décision du directeur (bourse, cas social, fratrie).
     * Seul statut posé manuellement. Le trigger ne l'écrase jamais.
     */
    WAIVED  ("Exonéré");

    private final String label;

    /** Frais à relancer par SMS (scheduler fee-reminder, 8h heure locale). */
    public boolean needsReminder() {
        return this == UNPAID || this == PARTIAL || this == OVERDUE;
    }

    /** Frais soldé : plus aucune allocation possible dessus. */
    public boolean isSettled() {
        return this == PAID || this == WAIVED;
    }
}


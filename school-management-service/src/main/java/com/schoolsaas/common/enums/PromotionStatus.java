package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Décision de passage en fin d'année scolaire (F-19).
 *
 * CORRECTION vs version précédente :
 *   - RETAINED    → REPEATED   (terminologie des specs)
 *   - GRADUATED   AJOUTÉ       (manquait ! F-19 : "Si dernière classe d'un niveau
 *                               → promotionStatus = GRADUATED")
 *   - CONDITIONAL → supprimé   (pas de règle métier définie dans les specs ;
 *                               un passage conditionnel se gère par un OVERRIDE
 *                               du directeur avec un commentaire)
 *   - OVERRIDDEN  → supprimé   (redondant : les colonnes promotion_validated_by
 *                               et promotion_validated_at tracent déjà qui a
 *                               validé/forcé la décision. Un override donne un
 *                               statut final PROMOTED ou REPEATED, pas un statut
 *                               "overridden" qui ne dit rien de la décision.)
 *
 * Type PostgreSQL correspondant : promotion_status
 */
@Getter
@RequiredArgsConstructor
public enum PromotionStatus {

    /** Décision pas encore prise. Statut par défaut à l'inscription. */
    PENDING   ("En attente"),

    /** Passage en classe supérieure (finalAverage >= moyenne de passage). */
    PROMOTED  ("Admis"),

    /** Redoublement : réinscription dans la même classe l'année suivante. */
    REPEATED  ("Redouble"),

    /** Fin de cycle : l'élève a terminé la dernière classe du dernier niveau. */
    GRADUATED ("Diplômé");

    private final String label;

    /** Une décision finale ne peut plus être modifiée sans override du directeur. */
    public boolean isFinal() {
        return this != PENDING;
    }
}


package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'une inscription d'élève pour une année scolaire donnée.
 *
 * CORRECTION vs version précédente :
 *   - ACTIVE      → ENROLLED   (aligné sur les specs F-09 et sur le code Java
 *                               qui filtre déjà sur 'enrolled')
 *   - INACTIVE    → supprimé   (redondant : une inscription non active est
 *                               soit TRANSFERRED, soit WITHDRAWN, soit GRADUATED)
 *   - DROPPED_OUT → WITHDRAWN  (un seul concept : l'élève a quitté l'école)
 *   - SUSPENDED   → supprimé   (une suspension est disciplinaire et temporaire,
 *                               elle relève de DisciplinaryRecord — V2 — pas du
 *                               statut d'inscription)
 *
 * ATTENTION : la vue matérialisée mv_dashboard_stats filtre sur ENROLLED.
 *
 * Type PostgreSQL correspondant : enrollment_status
 */
@Getter
@RequiredArgsConstructor
public enum EnrollmentStatus {

    /** Élève inscrit et actif dans la classe. Statut par défaut. */
    ENROLLED    ("Inscrit"),

    /** Élève parti vers un autre établissement (transfer_notes obligatoire). */
    TRANSFERRED ("Transféré"),

    /** Élève ayant quitté l'école en cours d'année (abandon, départ famille). */
    WITHDRAWN   ("Retiré"),

    /** Élève ayant terminé le cycle (dernière classe du dernier niveau). */
    GRADUATED   ("Diplômé");

    private final String label;

    /** Une inscription active est la seule à compter dans les effectifs et les frais. */
    public boolean isActive() {
        return this == ENROLLED;
    }
}


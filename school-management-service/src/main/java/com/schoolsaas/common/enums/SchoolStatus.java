package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'une école (tenant) sur la plateforme SaaS.
 *
 * CORRECTION : ces valeurs étaient en minuscules dans le script public
 * ('trial', 'active'...), alors que tout le reste du projet utilise des
 * MAJUSCULES. Or un enum Java annoté @Enumerated(EnumType.STRING) écrit le nom
 * de la constante — donc TRIAL, pas 'trial'. La contrainte CHECK aurait rejeté
 * le premier INSERT dès que School.status serait passé de String à enum.
 *
 * IMPACT SUR LE CODE : TenantInitializer et DashboardScheduler filtrent
 * actuellement avec List.of("trial", "active") — à remplacer par
 * List.of(SchoolStatus.TRIAL, SchoolStatus.ACTIVE).
 */
@Getter
@RequiredArgsConstructor
public enum SchoolStatus {

    /** Période d'essai (30 jours). L'école a accès à toutes les fonctionnalités. */
    TRIAL     ("Période d'essai"),

    /** Abonnement payé et en cours. */
    ACTIVE    ("Actif"),

    /** Suspendu pour impayé. Les données sont conservées, l'accès est bloqué. */
    SUSPENDED ("Suspendu"),

    /** Résilié. Le schema tenant est conservé pendant la durée de rétention légale. */
    DELETED   ("Supprimé");

    private final String label;

    /**
     * Une école opérationnelle : ses schedulers tournent, ses migrations sont
     * appliquées au démarrage, ses utilisateurs peuvent se connecter.
     * Utilisé par TenantInitializer, DashboardScheduler et FeeReminderScheduler.
     */
    public boolean isOperational() {
        return this == TRIAL || this == ACTIVE;
    }
}

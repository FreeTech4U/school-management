package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'un abonnement souscrit par une école.
 *
 * À ne pas confondre avec SchoolStatus : une école SUSPENDED a forcément un
 * abonnement EXPIRED ou CANCELLED, mais l'inverse n'est pas vrai — une école
 * peut rester ACTIVE quelques jours après l'expiration (période de grâce).
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionStatus {

    /** Abonnement en cours de validité. Un seul par école (contrainte en base). */
    ACTIVE    ("Actif"),

    /** Date de fin dépassée sans renouvellement. */
    EXPIRED   ("Expiré"),

    /** Résilié à la demande de l'école, avant sa date de fin. */
    CANCELLED ("Résilié");

    private final String label;
}

package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Périodicité de facturation d'un plan d'abonnement.
 *
 * CORRECTION : le script précédent portait une colonne price_monthly ET une
 * colonne billing_cycle, ce qui créait une ambiguïté irrésolvable — pour un
 * plan YEARLY, que valait price_monthly ? Le prix mensuel à multiplier par 12,
 * ou le prix annuel mal nommé ?
 *
 * Le modèle retenu sépare les deux prix (price_monthly et price_yearly). Le
 * billing_cycle porté par l'ABONNEMENT (et non par le plan) détermine lequel
 * s'applique. Cela permet à deux écoles de souscrire au même plan, l'une au
 * mois, l'autre à l'année.
 *
 * La facturation annuelle est le mode privilégié de la stratégie commerciale :
 * elle sécurise la trésorerie et réduit le taux de résiliation.
 */
@Getter
@RequiredArgsConstructor
public enum BillingCycle {

    MONTHLY ("Mensuel",  1),
    YEARLY  ("Annuel",  12);

    private final String label;

    /** Nombre de mois couverts par une échéance. Sert à calculer la date de fin. */
    private final int    months;
}

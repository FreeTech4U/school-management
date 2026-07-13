package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut d'une année scolaire.
 *
 * Règle métier (F-04) : une année CLOSED n'est plus modifiable.
 * Sa clôture déclenche la validation des promotions en attente (F-19).
 *
 * Type PostgreSQL correspondant : year_status
 */
@Getter
@RequiredArgsConstructor
public enum YearStatus {

    ACTIVE ("En cours"),
    CLOSED ("Clôturée");

    private final String label;
}

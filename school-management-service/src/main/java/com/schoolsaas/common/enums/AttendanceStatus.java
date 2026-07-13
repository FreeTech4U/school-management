package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut de présence d'un élève sur une demi-journée ou une journée (F-17).
 *
 * CORRECTION vs version précédente :
 *   La version précédente contenait 6 valeurs qui se chevauchaient :
 *   ABSENT, EXCUSED, JUSTIFIED et ABSENT_UNJUSTIFIED décrivaient tous une
 *   absence, avec deux axes mélangés (le fait d'être absent, et le fait que
 *   l'absence soit justifiée ou non).
 *
 *   Conséquence concrète : la règle SMS de F-17 devenait inapplicable.
 *   ("Après un status=ABSENT, pas EXCUSED : envoyer un SMS au parent"
 *    → et ABSENT_UNJUSTIFIED alors ? et JUSTIFIED ?)
 *
 *   → JUSTIFIED           supprimé (= EXCUSED)
 *   → ABSENT_UNJUSTIFIED  supprimé (= ABSENT, dont la justification est vide)
 *
 * MODÈLE RETENU — un seul axe :
 *   ABSENT  = absent, aucune justification connue au moment de l'appel
 *             → déclenche le SMS au parent
 *   EXCUSED = absence justifiée (le champ Attendance.justification est renseigné)
 *             → ne déclenche PAS de SMS
 *
 *   Une absence ABSENT peut être régularisée a posteriori : le directeur passe
 *   le statut à EXCUSED et renseigne la justification.
 *
 * Type PostgreSQL correspondant : attendance_status
 */
@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {

    /** Élève présent. */
    PRESENT ("Présent"),

    /** Absent sans justification. Déclenche un SMS au parent. */
    ABSENT  ("Absent"),

    /** Élève en retard mais présent. Pas de SMS. */
    LATE    ("En retard"),

    /** Absence justifiée (maladie, autorisation). Pas de SMS. */
    EXCUSED ("Absence justifiée");

    private final String label;

    /** Seule une absence non justifiée déclenche la notification au parent. */
    public boolean triggersParentSms() {
        return this == ABSENT;
    }

    /** Compte comme une absence dans les statistiques du bulletin. */
    public boolean isAbsence() {
        return this == ABSENT || this == EXCUSED;
    }
}

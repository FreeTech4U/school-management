package com.schoolsaas.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cycle de vie d'un bulletin scolaire (F-16).
 *
 * FLUX :
 *   DRAFT  --generate()-->  DRAFT (moyennes + rang calculés)
 *          --publish()  -->  PUBLISHED (PDF généré, publishedAt renseigné)
 *          --SMS parent -->  SENT_TO_PARENT
 *
 * CORRECTION vs version précédente :
 *   - SENT_TO_PARENT AJOUTÉ  (manquait ! F-16 étape 7 : "status → SENT_TO_PARENT"
 *                             après l'envoi du SMS. Sans cette valeur, impossible
 *                             de savoir si le parent a été notifié, et le système
 *                             risquerait de renvoyer le SMS en boucle.)
 *   - GENERATED → supprimé   (redondant avec DRAFT : la génération remplit les
 *                             moyennes mais le bulletin reste un brouillon
 *                             modifiable tant qu'il n'est pas publié)
 *   - ARCHIVED  → supprimé   (l'archivage se déduit du statut CLOSED de l'année
 *                             scolaire, pas besoin d'un statut dédié)
 *   - CORRECTED → supprimé   (une correction repasse le bulletin en DRAFT puis
 *                             le republie — pas besoin d'un statut terminal)
 *
 * Type PostgreSQL correspondant : report_card_status
 */
@Getter
@RequiredArgsConstructor
public enum ReportCardStatus {

    /** Brouillon : moyennes calculées, encore modifiable (appréciations). */
    DRAFT          ("Brouillon"),

    /** Publié : PDF généré et figé. Consultable par le parent. */
    PUBLISHED      ("Publié"),

    /** SMS de notification envoyé au parent. État terminal. */
    SENT_TO_PARENT ("Envoyé au parent");

    private final String label;

    /** Un bulletin publié n'est plus modifiable sans repasser en DRAFT. */
    public boolean isPublished() {
        return this == PUBLISHED || this == SENT_TO_PARENT;
    }
}


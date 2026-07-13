package com.schoolsaas.communication.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.SmsCategory;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle de SMS (F-13).
 *
 * Les variables au format {{nom}} sont résolues à l'envoi par SmsTemplateEngine.
 * Quatre modèles sont pré-chargés à la création du schema tenant :
 *   fee_reminder · payment_received · report_card_published · absence_notification
 *
 * CORRECTIONS :
 *   • AJOUT du champ isActive : la colonne existait en base (NOT NULL DEFAULT
 *     TRUE) mais pas dans l'entité. Elle permet au directeur de désactiver un
 *     modèle sans le supprimer — par exemple suspendre les notifications
 *     d'absence pendant les vacances.
 *
 *   • variables passe d'un String à une List<String> typée.
 *     Le code précédent déclarait :
 *         @Column(columnDefinition = "JSONB") private String variables;
 *     Hibernate envoyait alors un paramètre varchar vers une colonne jsonb :
 *         ERROR: column "variables" is of type jsonb
 *                but expression is of type character varying
 *
 *     DEUX SOLUTIONS possibles :
 *       (a) @Type(JsonType.class) — nécessite la dépendance Maven
 *           io.hypersistence:hypersistence-utils-hibernate-63
 *       (b) @JdbcTypeCode(SqlTypes.JSON) — natif Hibernate 6, sans dépendance,
 *           mais le mapping vers une List<String> demande un converter.
 *     La solution (a) est retenue ici : elle mappe directement la liste.
 */
@Entity
@Table(name = "sms_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplate extends BaseEntity {

    /** fee_reminder, payment_received, report_card_published... */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** FINANCIAL · ACADEMIC · ADMINISTRATIVE · CUSTOM */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmsCategory category;

    /** Contenu du message, avec ses variables {{…}} non résolues. */
    @Column(name = "content_fr", nullable = false, columnDefinition = "TEXT")
    private String contentFr;

    /** Liste des variables attendues, stockée en JSONB. */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> variables = new ArrayList<>();

    /** AJOUT — colonne présente en base, absente de l'entité précédente. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

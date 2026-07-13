package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import com.schoolsaas.common.enums.SchoolStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * École (tenant) de la plateforme SaaS.
 *
 * ⚠ Cette entité vit dans le schema PUBLIC — d'où @Table(schema = "public").
 *   Les entités des autres domaines n'ont PAS d'attribut schema : elles vivent
 *   dans le schema tenant courant, résolu dynamiquement par Hibernate via le
 *   TenantContext.
 *
 * schemaName est concaténé dans des requêtes SQL (SET search_path,
 * CREATE SCHEMA, REFRESH MATERIALIZED VIEW) car PostgreSQL n'autorise pas les
 * paramètres préparés pour les noms d'objets. Une contrainte CHECK en base
 * (^[a-z0-9_]+$) et une validation regex côté Java protègent de l'injection.
 */
@Entity
@Table(name = "schools", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class School extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    /** Identifiant public de l'école, saisi au login. Ex : ste-marie-dixinn. */
    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    /**
     * Nom du schema PostgreSQL dédié. Ex : tenant_ste_marie.
     * VARCHAR(63) : limite stricte de PostgreSQL pour un identifiant.
     */
    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    private String schemaName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(name = "country_code", nullable = false, length = 3)
    @Builder.Default
    private String countryCode = "GN";

    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * TRIAL · ACTIVE · SUSPENDED · DELETED
     * CORRECTION : les valeurs étaient en minuscules ('trial', 'active') dans
     * l'ancien script SQL. Un enum Java annoté @Enumerated(EnumType.STRING)
     * écrit le NOM de la constante — donc TRIAL. La contrainte CHECK aurait
     * rejeté le premier INSERT.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SchoolStatus status = SchoolStatus.TRIAL;

    /**
     * Fuseau horaire de l'école. DONNÉE MÉTIER, pas réglage technique.
     *
     * Les schedulers se déclenchent toutes les heures en UTC et filtrent les
     * écoles pour lesquelles il est actuellement l'heure locale voulue
     * (8h pour les rappels SMS, minuit pour la bascule des frais en OVERDUE).
     * Une école à Conakry et une à Abidjan reçoivent ainsi leurs SMS à 8h
     * CHEZ ELLES, avec un seul scheduler.
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Africa/Conakry";

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "GNF";

    /** TenantInitializer et les schedulers ne traitent que ces écoles. */
    @Transient
    public boolean isOperational() {
        return status != null && status.isOperational();
    }
}

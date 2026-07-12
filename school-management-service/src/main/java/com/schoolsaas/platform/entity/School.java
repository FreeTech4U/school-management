package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Représente une école (tenant) dans la plateforme SchoolSaaS.
 * Chaque école possède son propre schéma de base de données pour l'isolation des données.
 */
@Entity
@Table(name = "schools", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class School extends BaseEntity {

    /** Nom de l'école */
    @Column(nullable = false)
    private String name;

    /** Identifiant unique textuel utilisé dans les URLs */
    @Column(unique = true, nullable = false)
    private String slug;

    /** Nom du schéma PostgreSQL dédié à cette école */
    @Column(name = "schema_name", unique = true, nullable = false)
    private String schemaName;

    /** Adresse email de contact principal */
    @Column(unique = true, nullable = false)
    private String email;

    /** Numéro de téléphone de contact */
    private String phone;

    /** Adresse physique de l'école */
    private String address;

    /** Ville de résidence */
    private String city;

    /** Code pays (ex: GN pour la Guinée) */
    @Column(name = "country_code", length = 3)
    private String countryCode = "GN";

    /** URL vers le logo de l'école */
    @Column(name = "logo_url")
    private String logoUrl;

    /** Statut actuel (trial, active, suspended, deleted) */
    @Column(nullable = false)
    private String status = "trial"; // trial, active, suspended, deleted

    /** Fuseau horaire utilisé pour les opérations académiques */
    private String timezone = "Africa/Conakry";

    /** Devise monétaire par défaut pour la finance */
    private String currency = "GNF";
}

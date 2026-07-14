package com.schoolsaas.platform.entity;

import com.schoolsaas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Rôle applicatif — remplace l'ancien enum Java com.schoolsaas.common.enums.Role.
 *
 * ⚠ Schema PUBLIC. Catalogue UNIQUE, partagé par toutes les écoles.
 *
 * Les rôles ne sont plus un vocabulaire figé au moment de la compilation :
 * ils deviennent une DONNÉE, gérable via une future API d'administration
 * (ajout/désactivation d'un rôle sans migration ni redéploiement).
 *
 * La suppression d'un rôle est modélisée comme une désactivation (isActive),
 * jamais une suppression réelle : un rôle déjà assigné à des utilisateurs
 * dans plusieurs écoles ne peut pas disparaître sans casser ces assignations.
 */
@Entity
@Table(name = "roles", schema = "public")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    /** Code stable, utilisé par le code applicatif et le claim JWT "roles". */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

package com.schoolsaas.platform.repository;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.platform.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour la gestion des écoles (tenants) dans le schéma public.
 */
public interface SchoolRepository extends JpaRepository<School, UUID> {

    /** Récupère une école par son slug unique */
    Optional<School> findBySlug(String slug);

    /** Vérifie si un slug est déjà utilisé */
    boolean existsBySlug(String slug);

    /** Vérifie si un email est déjà utilisé */
    boolean existsByEmail(String email);

    /** Récupère une école par son slug et ses statuts autorisés */
    Optional<School> findBySlugAndStatusIn(String slug, Collection<SchoolStatus> statuses);

    /** Récupère toutes les écoles ayant certains statuts */
    List<School> findAllByStatusIn(Collection<SchoolStatus> statuses);

    Optional<School> findBySchemaNameAndStatusIn(String schemaName, Collection<SchoolStatus> statuses);
}

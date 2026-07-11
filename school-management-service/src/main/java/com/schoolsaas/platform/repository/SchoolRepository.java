package com.schoolsaas.platform.repository;

import com.schoolsaas.platform.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByEmail(String email);
    Optional<School> findBySlugAndStatusIn(String slug, java.util.Collection<String> statuses);
    List<School> findAllByStatusIn(java.util.Collection<String> statuses);
}

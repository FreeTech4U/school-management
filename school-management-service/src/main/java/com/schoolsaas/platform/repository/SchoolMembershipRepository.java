package com.schoolsaas.platform.repository;

import com.schoolsaas.platform.entity.SchoolMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolMembershipRepository extends JpaRepository<SchoolMembership, UUID> {

    /**
     * Écoles actives ET opérationnelles (ACTIVE/TRIAL) d'une personne.
     * LA requête centrale du login multi-écoles.
     * JOIN FETCH sur school : anti N+1.
     */
    @Query("""
        SELECT m FROM SchoolMembership m
        JOIN FETCH m.school s
        WHERE m.person.id = :personId
          AND m.isActive = true
          AND s.status IN (
              com.schoolsaas.common.enums.SchoolStatus.ACTIVE,
              com.schoolsaas.common.enums.SchoolStatus.TRIAL
          )
        """)
    List<SchoolMembership> findActiveOperationalByPersonId(@Param("personId") UUID personId);

    Optional<SchoolMembership> findByPersonIdAndSchoolId(UUID personId, UUID schoolId);
}
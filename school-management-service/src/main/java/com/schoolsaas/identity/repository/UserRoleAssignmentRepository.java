package com.schoolsaas.identity.repository;

import com.schoolsaas.identity.entity.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {

    /**
     * Ne renvoie QUE les roleId (des UUID vers public.roles), jamais une
     * jointure vers la table roles elle-même : identity/ ne doit pas écrire
     * de requête touchant le schema public. La résolution des codes se fait
     * ensuite via platform/RoleCatalogService.
     */
    @Query("SELECT ura.roleId FROM UserRoleAssignment ura WHERE ura.user.id = :userId")
    List<UUID> findRoleIdsByUserId(@Param("userId") UUID userId);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}

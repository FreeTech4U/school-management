package com.schoolsaas.platform.service;

import com.schoolsaas.platform.entity.Role;
import com.schoolsaas.platform.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Point d'accès UNIQUE pour tout domaine ayant besoin de résoudre des rôles
 * sans lire directement public.roles.
 *
 * RÈGLE D'ARCHITECTURE : un domaine appelle les SERVICES d'un autre domaine,
 * jamais son repository ni ses entités JPA directement. identity/
 * (AuthTenantService) et tout futur domaine ayant besoin de codes de rôles
 * passent par ICI — jamais par RoleRepository directement, sauf depuis
 * platform/ lui-même (intra-domaine, ex : OnboardingService).
 */
@Service
@RequiredArgsConstructor
public class RoleCatalogService {

    private final RoleRepository roleRepository;

    /**
     * Résout des codes de rôle à partir de leurs UUID, en un seul appel —
     * anti-N+1. Utilisé par AuthTenantService à chaque connexion pour
     * construire la liste des rôles du JWT.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> getCodesByIds(Collection<UUID> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));
    }

    /** Résout l'UUID d'un rôle à partir de son code (ex : bootstrap DIRECTOR). */
    @Transactional(readOnly = true)
    public UUID getIdByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        "Rôle introuvable dans public.roles : " + code))
                .getId();
    }

    @Transactional(readOnly = true)
    public List<Role> getActiveRoles() {
        return roleRepository.findAllByIsActiveTrue();
    }
}

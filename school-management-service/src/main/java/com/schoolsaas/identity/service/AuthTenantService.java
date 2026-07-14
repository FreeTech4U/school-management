package com.schoolsaas.identity.service;

import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.config.security.AuthenticatedUser;
import com.schoolsaas.config.security.JwtService;
import com.schoolsaas.identity.dto.request.LoginRequest;
import com.schoolsaas.identity.dto.response.AuthResponse;
import com.schoolsaas.identity.entity.User;
import com.schoolsaas.identity.repository.UserRepository;

import com.schoolsaas.identity.repository.UserRoleAssignmentRepository;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.service.RoleCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Opérations d'authentification qui touchent le schema TENANT.
 *
 * RÉSOLUTION DES RÔLES — EN DEUX ÉTAPES, CHACUNE DANS SON DOMAINE
 *
 *   1. identity/  : UserRoleAssignmentRepository.findRoleIdsByUserId(userId)
 *                   → une liste de UUID, requête intra-domaine (identity →
 *                     identity), aucune jointure vers public.roles ici.
 *
 *   2. platform/  : RoleCatalogService.getCodesByIds(roleIds)
 *                   → résout ces UUID en codes ("DIRECTOR", "TEACHER"...),
 *                     via le SERVICE du domaine propriétaire de Role — jamais
 *                     via une requête directe sur RoleRepository depuis ici.
 *
 * Cette classe ne référence JAMAIS l'entité Role ni RoleRepository
 * directement : uniquement RoleCatalogService, le point de passage du
 * domaine platform/.
 *
 * Toute méthode ici DOIT être appelée depuis un bean différent (AuthService),
 * jamais en interne : @Transactional ne s'applique que sur les appels
 * externes, qui passent par le proxy Spring.
 */
@Service
@RequiredArgsConstructor
public class AuthTenantService {

    private final UserRepository              userRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final RoleCatalogService roleCatalogService;
    private final PasswordEncoder              passwordEncoder;
    private final JwtService                   jwtService;

    @Value("${app.jwt.access-token-expiration}")
    private long jwtExpiration;

    @Transactional
    public AuthResponse authenticate(LoginRequest request, School school, boolean fallbackOccurred) {

        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> BusinessException.unauthorized(
                        "INVALID_CREDENTIALS", "Identifiants incorrects"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("INVALID_CREDENTIALS", "Identifiants incorrects");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roleCodes = resolveRoleCodes(user.getId());

        return buildAuthResponse(user, school, roleCodes, fallbackOccurred);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshTokens(UUID userId, String tenantSchema) {

        User user = userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> BusinessException.unauthorized(
                        "INVALID_TOKEN", "Utilisateur introuvable ou désactivé"));

        List<String> roleCodes = resolveRoleCodes(user.getId());

        AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .tenantId(tenantSchema)
                .roles(roleCodes)
                .build();

        String accessToken     = jwtService.generateToken(authenticatedUser);
        String newRefreshToken = jwtService.generateRefreshToken(authenticatedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    /**
     * Résolution en deux étapes — voir Javadoc de classe.
     * Aucun risque de N+1 : un seul appel par étape, quel que soit le nombre
     * de rôles de l'utilisateur (2 à 4 en pratique).
     */
    private List<String> resolveRoleCodes(UUID userId) {
        List<UUID> roleIds = userRoleAssignmentRepository.findRoleIdsByUserId(userId);
        Map<UUID, String> codesById = roleCatalogService.getCodesByIds(roleIds);
        return roleIds.stream().map(codesById::get).filter(java.util.Objects::nonNull).toList();
    }

    private AuthResponse buildAuthResponse(
            User user, School school, List<String> roleCodes, boolean fallbackOccurred) {

        AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .tenantId(school.getSchemaName())
                .roles(roleCodes)
                .build();

        String accessToken  = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpiration / 1000)
                .redirectedToFallbackSchool(fallbackOccurred)
                .user(AuthResponse.UserData.builder()
                        .id(user.getId())
                        .fullName(user.getFirstName() + " " + user.getLastName())
                        .email(user.getEmail())
                        .roles(roleCodes)
                        .tenantId(school.getId())
                        .schoolName(school.getName())
                        .schoolSlug(school.getSlug())
                        .build())
                .build();
    }

    public boolean isTokenValid(String token) {
        return jwtService.isTokenValid(token);
    }

    public boolean isRefreshTokenType(String token) {
        String type = jwtService.extractClaim(token, claims -> (String) claims.get("type"));
        return "refresh".equals(type);
    }

    public String extractTenantSchema(String token) {
        return jwtService.extractTenantId(token);
    }

    public UUID extractUserId(String token) {
        String userId = jwtService.extractClaim(token, claims -> (String) claims.get("userId"));
        return UUID.fromString(userId);
    }
}

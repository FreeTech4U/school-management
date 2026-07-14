package com.schoolsaas.identity.service;

import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.config.security.AuthenticatedUser;
import com.schoolsaas.config.security.JwtService;
import com.schoolsaas.identity.dto.request.LoginRequest;
import com.schoolsaas.identity.dto.response.AuthResponse;
import com.schoolsaas.identity.entity.User;
import com.schoolsaas.identity.repository.UserRepository;

import com.schoolsaas.platform.entity.School;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Opérations d'authentification qui touchent le schema TENANT.
 *
 * Toute méthode ici DOIT être appelée depuis un bean différent (AuthService),
 * jamais en interne. @Transactional ne s'applique que sur les appels externes,
 * qui passent par le proxy Spring — c'est ce qui garantit qu'une session
 * Hibernate neuve s'ouvre à chaque appel, AVEC le TenantContext déjà positionné
 * par l'appelant.
 */
@Service
@RequiredArgsConstructor
public class AuthTenantService {

    private final UserRepository   userRepository;
    private final PasswordEncoder  passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.access-token-expiration}")
    private long jwtExpiration;

    /**
     * Vérifie les identifiants et émet les tokens (F-02a).
     *
     * Le message d'erreur reste IDENTIQUE que l'utilisateur soit introuvable ou
     * que le mot de passe soit incorrect — ne jamais indiquer lequel des deux a
     * échoué, pour ne pas confirmer l'existence d'un compte à un attaquant.
     * Ce comportement, déjà correct dans la version précédente, est conservé.
     */
    @Transactional
    public AuthResponse authenticate(LoginRequest request, School school) {

        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> BusinessException.unauthorized(
                        "INVALID_CREDENTIALS", "Identifiants incorrects"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("INVALID_CREDENTIALS", "Identifiants incorrects");
        }

        // user est une entité managée (chargée dans CETTE session) : la
        // modification est répercutée au flush de fin de transaction par le
        // dirty checking d'Hibernate. Le save() explicite est sans effet
        // néfaste mais redondant — conservé par lisibilité/explicité.
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // NOTE DE NOMMAGE : AuthenticatedUser.tenantId porte ici le NOM DU
        // SCHEMA (ex: "tenant_ste_marie"), consommé par JwtAuthenticationFilter
        // pour repositionner TenantContext sur les requêtes futures — à ne pas
        // confondre avec AuthResponse.UserData.tenantId ci-dessous, qui porte
        // l'UUID de l'école (identifiant métier, consommé par le frontend).
        // Même nom de champ, deux sens différents selon la classe : à garder en
        // tête si Junie ou un futur développeur touche ce code.
        AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .tenantId(school.getSchemaName())
                .roles(List.of(user.getRole().name()))
                .build();

        String accessToken  = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpiration / 1000)
                .user(AuthResponse.UserData.builder()
                        .id(user.getId())
                        .fullName(user.getFirstName() + " " + user.getLastName())
                        .email(user.getEmail())
                        .roles(List.of(user.getRole().name()))
                        .tenantId(school.getId())      // UUID — voir note ci-dessus
                        .schoolName(school.getName())
                        .build())
                .build();
    }

    /**
     * Revalide l'utilisateur en base et émet de nouveaux tokens (F-02b).
     *
     * CORRECTION : contrairement à la version précédente qui ne faisait que
     * recopier les claims du vieux token, cette méthode relit systématiquement
     * l'utilisateur — garantissant que son statut actif et son rôle courant
     * (pas celui figé dans l'ancien token) déterminent les nouveaux tokens émis.
     * La vérification du statut de l'école a déjà été faite par l'appelant
     * (AuthService.refresh(), avant le positionnement du TenantContext).
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshTokens(UUID userId, String tenantSchema) {

        User user = userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> BusinessException.unauthorized(
                        "INVALID_TOKEN", "Utilisateur introuvable ou désactivé"));

        AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .tenantId(tenantSchema)
                .roles(List.of(user.getRole().name()))   // rôle ACTUEL, pas celui du vieux token
                .build();

        String accessToken     = jwtService.generateToken(authenticatedUser);
        String newRefreshToken = jwtService.generateRefreshToken(authenticatedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Utilitaires JWT — ne touchent pas la base, pas besoin de tenant
    // ─────────────────────────────────────────────────────────────────────

    public boolean isTokenValid(String token) {
        return jwtService.isTokenValid(token);
    }

    /**
     * CORRECTION : vérifie que le token est bien de type "refresh".
     * Sans ce contrôle, un access token (courte durée de vie voulue : 1h)
     * pouvait être soumis à /auth/refresh pour obtenir indéfiniment de
     * nouveaux tokens, contournant sa durée de vie prévue.
     */
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
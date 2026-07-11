package com.schoolsaas.identity.service;

import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.config.multitenancy.TenantContext;
import com.schoolsaas.config.security.AuthenticatedUser;
import com.schoolsaas.config.security.JwtService;
import com.schoolsaas.identity.dto.request.LoginRequest;
import com.schoolsaas.identity.dto.response.AuthResponse;
import com.schoolsaas.identity.entity.User;
import com.schoolsaas.identity.repository.UserRepository;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.access-token-expiration}")
    private long jwtExpiration;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {} in tenant: {}", request.getEmail(), request.getTenantSlug());

        // 1. Load School
        School school = schoolRepository.findBySlugAndStatusIn(request.getTenantSlug(), List.of("active", "trial"))
                .orElseThrow(() -> BusinessException.notFound("TENANT_NOT_FOUND", "École inactive ou inexistante"));

        // 2. Set Tenant Context
        TenantContext.set(school.getSchemaName());

        try {
            // 3. Load User
            User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Identifiants incorrects"));

            // 4. Verify Password
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new BadCredentialsException("Identifiants incorrects");
            }

            // 5. Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // 6. Generate Tokens
            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .tenantId(school.getSchemaName())
                    .roles(List.of(user.getRole()))
                    .build();

            String accessToken = jwtService.generateToken(authenticatedUser);
            String refreshToken = jwtService.generateRefreshToken(authenticatedUser);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtExpiration / 1000)
                    .user(AuthResponse.UserData.builder()
                            .id(user.getId())
                            .fullName(user.getFirstName() + " " + user.getLastName())
                            .email(user.getEmail())
                            .roles(List.of(user.getRole()))
                            .tenantId(school.getId())
                            .schoolName(school.getName())
                            .build())
                    .build();
        } finally {
            TenantContext.clear();
        }
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BadCredentialsException("Token de rafraîchissement invalide ou expiré");
        }

        String email = jwtService.extractUsername(refreshToken);
        String tenantId = jwtService.extractTenantId(refreshToken);
        List<String> roles = jwtService.extractRoles(refreshToken);
        String userId = jwtService.extractClaim(refreshToken, claims -> (String) claims.get("userId"));

        AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                .userId(java.util.UUID.fromString(userId))
                .email(email)
                .tenantId(tenantId)
                .roles(roles)
                .build();

        String accessToken = jwtService.generateToken(authenticatedUser);
        String newRefreshToken = jwtService.generateRefreshToken(authenticatedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtExpiration / 1000)
                .build();
    }
}

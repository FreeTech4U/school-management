package com.schoolsaas.identity.service;

import com.schoolsaas.common.enums.Role;
import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.config.security.AuthenticatedUser;
import com.schoolsaas.config.security.JwtService;
import com.schoolsaas.identity.dto.request.LoginRequest;
import com.schoolsaas.identity.dto.response.AuthResponse;
import com.schoolsaas.identity.entity.User;
import com.schoolsaas.identity.repository.UserRepository;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password");
        request.setTenantSlug("test-school");

        School school = new School();
        school.setSchemaName("school_test");
        school.setStatus(SchoolStatus.ACTIVE);
        school.setName("Test School");

        User user = new User();
        user.setEmail("user@test.com");
        user.setPasswordHash("hashed_password");
        user.setRole(Role.DIRECTOR);
        user.setFirstName("John");
        user.setLastName("Doe");

        when(schoolRepository.findBySlugAndStatusIn(any(), any())).thenReturn(Optional.of(school));
        when(userRepository.findByEmailAndIsActiveTrue(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("John Doe", response.getUser().getFullName());
    }

    @Test
    void login_SchoolNotFound_ThrowsException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setTenantSlug("unknown");

        when(schoolRepository.findBySlugAndStatusIn(any(), any())).thenReturn(Optional.empty());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("TENANT_NOT_FOUND", ex.getCode());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong");
        request.setTenantSlug("test-school");

        School school = new School();
        school.setStatus(SchoolStatus.ACTIVE);

        User user = new User();
        user.setPasswordHash("hashed");

        when(schoolRepository.findBySlugAndStatusIn(any(), any())).thenReturn(Optional.of(school));
        when(userRepository.findByEmailAndIsActiveTrue(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}

package com.schoolsaas.identity.service;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.identity.dto.request.LoginRequest;
import com.schoolsaas.identity.dto.response.AuthResponse;
import com.schoolsaas.platform.entity.Person;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.entity.SchoolMembership;
import com.schoolsaas.platform.repository.PersonRepository;
import com.schoolsaas.platform.repository.SchoolMembershipRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PersonRepository personRepository;
    @Mock
    private SchoolMembershipRepository membershipRepository;
    @Mock
    private AuthTenantService authTenantService;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuthService authService;

    private Person person;
    private School school;
    private SchoolMembership membership;

    private void givenPersonWithOneActiveSchool() {
        person = new Person();
        person.setId(UUID.randomUUID());
        person.setEmail("user@test.com");

        school = new School();
        school.setId(UUID.randomUUID());
        school.setSlug("test-school");
        school.setSchemaName("school_test");
        school.setStatus(SchoolStatus.ACTIVE);
        school.setName("Test School");

        membership = SchoolMembership.builder()
                .person(person)
                .school(school)
                .tenantUserId(UUID.randomUUID())
                .isActive(true)
                .build();

        when(personRepository.findByEmail(person.getEmail())).thenReturn(Optional.of(person));
        when(membershipRepository.findActiveOperationalByPersonId(person.getId()))
                .thenReturn(List.of(membership));
    }

    @Test
    void login_Success() {
        // Given
        givenPersonWithOneActiveSchool();

        LoginRequest request = new LoginRequest();
        request.setEmail(person.getEmail());
        request.setPassword("password");
        request.setTenantSlug(school.getSlug());

        AuthResponse expectedResponse = AuthResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .user(AuthResponse.UserData.builder().fullName("John Doe").build())
                .build();

        when(authTenantService.authenticate(eq(request), eq(school), eq(false)))
                .thenReturn(expectedResponse);
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(entityManager.getReference(School.class, school.getId())).thenReturn(school);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("John Doe", response.getUser().getFullName());
    }

    @Test
    void login_PersonNotFound_ThrowsException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@test.com");
        request.setPassword("password");

        when(personRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Given
        givenPersonWithOneActiveSchool();

        LoginRequest request = new LoginRequest();
        request.setEmail(person.getEmail());
        request.setPassword("wrong");
        request.setTenantSlug(school.getSlug());

        when(authTenantService.authenticate(eq(request), eq(school), eq(false)))
                .thenThrow(BusinessException.unauthorized("INVALID_CREDENTIALS", "Identifiants incorrects"));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }
}

package com.schoolsaas.platform.service;

import com.schoolsaas.common.constants.SystemRoleCodes;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.platform.dto.request.OnboardingRequest;
import com.schoolsaas.platform.dto.response.OnboardingResponse;
import com.schoolsaas.platform.entity.Person;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.entity.SubscriptionPlan;
import com.schoolsaas.platform.repository.PersonRepository;
import com.schoolsaas.platform.repository.SchoolMembershipRepository;
import com.schoolsaas.platform.repository.SchoolRepository;
import com.schoolsaas.platform.repository.SchoolSubscriptionRepository;
import com.schoolsaas.platform.repository.SubscriptionPlanRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private SubscriptionPlanRepository planRepository;
    @Mock
    private SchoolSubscriptionRepository subscriptionRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private SchoolMembershipRepository membershipRepository;
    @Mock
    private RoleCatalogService roleCatalogService;
    @Mock
    private TenantMigrationService migrationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void onboard_Success() {
        // Given
        OnboardingRequest request = new OnboardingRequest();
        request.setSchoolName("Test School");
        request.setSlug("test-school");
        request.setEmail("admin@test.com");
        request.setPlanCode("BASIC");
        request.setDirectorFirstName("John");
        request.setDirectorLastName("Doe");
        request.setDirectorPassword("password");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode("BASIC");

        when(schoolRepository.existsBySlug(anyString())).thenReturn(false);
        when(planRepository.findByCode("BASIC")).thenReturn(Optional.of(plan));
        when(schoolRepository.save(any(School.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(roleCatalogService.getIdByCode(SystemRoleCodes.DIRECTOR)).thenReturn(UUID.randomUUID());
        when(personRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));
        when(membershipRepository.findByPersonIdAndSchoolId(any(), any())).thenReturn(Optional.empty());

        // When
        OnboardingResponse response = onboardingService.onboard(request);

        // Then
        assertNotNull(response);
        assertEquals("test-school", response.getTenantSlug());
        verify(jdbcTemplate).execute(contains("CREATE SCHEMA"));
        verify(migrationService).migrateTenant(anyString());
        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void onboard_DuplicateSlug_ThrowsException() {
        // Given
        OnboardingRequest request = new OnboardingRequest();
        request.setSlug("existing-school");

        when(schoolRepository.existsBySlug("existing-school")).thenReturn(true);

        // When & Then
        assertThrows(BusinessException.class, () -> onboardingService.onboard(request));
    }
}

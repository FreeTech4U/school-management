package com.schoolsaas.platform.service;

import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.platform.dto.request.OnboardingRequest;
import com.schoolsaas.platform.dto.response.OnboardingResponse;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.entity.SubscriptionPlan;
import com.schoolsaas.platform.repository.SchoolRepository;
import com.schoolsaas.platform.repository.SchoolSubscriptionRepository;
import com.schoolsaas.platform.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

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
    private TenantMigrationService migrationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JdbcTemplate jdbcTemplate;

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
        when(schoolRepository.existsByEmail(anyString())).thenReturn(false);
        when(planRepository.findByCode("BASIC")).thenReturn(Optional.of(plan));
        when(schoolRepository.save(any(School.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

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

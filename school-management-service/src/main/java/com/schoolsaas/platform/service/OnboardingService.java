package com.schoolsaas.platform.service;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.common.enums.SubscriptionStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.common.util.SlugUtils;
import com.schoolsaas.platform.dto.request.OnboardingRequest;
import com.schoolsaas.platform.dto.response.OnboardingResponse;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.entity.SchoolSubscription;
import com.schoolsaas.platform.entity.SubscriptionPlan;
import com.schoolsaas.platform.repository.SchoolRepository;
import com.schoolsaas.platform.repository.SchoolSubscriptionRepository;
import com.schoolsaas.platform.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service responsable de l'onboarding des nouvelles écoles sur la plateforme.
 * Gère la création de l'école, de son schéma de base de données, de son abonnement
 * et du compte utilisateur administrateur (Directeur).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final SchoolRepository schoolRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SchoolSubscriptionRepository subscriptionRepository;
    private final TenantMigrationService migrationService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Initialise une nouvelle école et son environnement technique complet.
     *
     * @param request Données d'inscription de l'école et du directeur.
     * @return Réponse contenant les détails de l'onboarding réussi.
     * @throws BusinessException si le slug ou l'email est déjà utilisé.
     */
    @Transactional
    public OnboardingResponse onboard(OnboardingRequest request) {
        log.info("Starting onboarding for school: {}", request.getSchoolName());

        // 1. Validations
        if (schoolRepository.existsBySlug(request.getSlug())) {
            throw BusinessException.conflict("SLUG_ALREADY_TAKEN", "Ce slug est déjà utilisé");
        }
        if (schoolRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("EMAIL_ALREADY_TAKEN", "Cet email est déjà utilisé");
        }

        SubscriptionPlan plan = planRepository.findByCode(request.getPlanCode())
                .orElseThrow(() -> new BusinessException("PLAN_NOT_FOUND", "Plan de souscription introuvable"));

        // 2. Create School
        String schemaName = SlugUtils.toSchemaName(request.getSlug());
        School school = School.builder()
                .name(request.getSchoolName())
                .slug(request.getSlug())
                .schemaName(schemaName)
                .email(request.getEmail())
                .phone(request.getPhone())
                .city(request.getCity())
                .countryCode(request.getCountryCode())
                .status(SchoolStatus.TRIAL)
                .timezone(request.getTimezone())
                .currency(request.getCurrency())
                .build();
        school = schoolRepository.save(school);

        // 3. Create Subscription
        SchoolSubscription subscription = SchoolSubscription.builder()
                .school(school)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .status(SubscriptionStatus.ACTIVE)
                .autoRenew(true)
                .build();
        subscriptionRepository.save(subscription);

        // 4. Create Schema
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        // 5. Migrate Schema
        migrationService.migrateTenant(schemaName);

        // 6. Create Director in Tenant Schema
        createDirector(schemaName, request);

        return OnboardingResponse.builder()
                .tenantSlug(school.getSlug())
                .schemaName(schemaName)
                .schoolName(school.getName())
                .message("École créée. Connectez-vous avec votre email directeur.")
                .build();
    }

    /**
     * Crée le compte utilisateur du directeur dans le schéma spécifique de l'école.
     *
     * @param schemaName Nom du schéma PostgreSQL de l'école.
     * @param request Données de l'onboarding contenant les infos du directeur.
     */
    private void createDirector(String schemaName, OnboardingRequest request) {
        String sql = "INSERT INTO " + schemaName + ".users (first_name, last_name, email, password_hash, role, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                request.getDirectorFirstName(),
                request.getDirectorLastName(),
                request.getEmail(),
                passwordEncoder.encode(request.getDirectorPassword()),
                "DIRECTOR",
                true
        );
    }
}

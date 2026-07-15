package com.schoolsaas.platform.service;

import com.schoolsaas.platform.entity.*;
import com.schoolsaas.platform.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import com.schoolsaas.common.enums.BillingCycle;
import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.common.enums.SubscriptionStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.common.util.SchemaNameValidator;
import com.schoolsaas.common.util.SlugUtils;
import com.schoolsaas.platform.dto.request.OnboardingRequest;
import com.schoolsaas.platform.dto.response.OnboardingResponse;
import  com.schoolsaas.common.constants.SystemRoleCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsable de l'onboarding des nouvelles écoles (F-01) et de leur
 * rattachement au compte multi-écoles d'une personne.
 *
 * CORRECTION vs version précédente — les rôles ne sont plus un enum Java fixe
 * (com.schoolsaas.common.enums.Role, supprimé). Le rôle DIRECTOR attribué au
 * créateur de l'école est désormais résolu par son CODE (SystemRoleCodes.
 * DIRECTOR) auprès de public.roles, dont l'id (UUID) est ensuite inséré dans
 * <schema_tenant>.user_roles.role_id — une clé étrangère inter-schema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final SchoolRepository             schoolRepository;
    private final SubscriptionPlanRepository   planRepository;
    private final SchoolSubscriptionRepository subscriptionRepository;
    private final PersonRepository      personRepository;
    private final SchoolMembershipRepository    membershipRepository;
    private final RoleCatalogService             roleCatalogService;
    private final TenantMigrationService        migrationService;
    private final PasswordEncoder               passwordEncoder;
    private final JdbcTemplate                  jdbcTemplate;
    private final EntityManager entityManager;

    private static final int TRIAL_DURATION_DAYS = 30;

    // =========================================================================
    // POINT D'ENTRÉE PRINCIPAL
    // =========================================================================

    public OnboardingResponse onboard(OnboardingRequest request) {
        log.info("Démarrage de l'onboarding pour l'école : {}", request.getSchoolName());

        validateUniqueness(request.getSlug());
        SubscriptionPlan plan = findPlanOrThrow(request.getPlanCode());
        String schemaName = SlugUtils.toSchemaName(request.getSlug());
        validateSchemaName(schemaName);

        // Étape 1 — écritures JPA (school + subscription), COMMITÉES avant de
        // passer au DDL (voir Javadoc historique : évite le deadlock Flyway).
        School school = createSchoolAndSubscription(request, plan, schemaName);

        try {
            // Étape 2 — DDL : schema + migrations Flyway.
            provisionTenantSchema(schemaName);

            // Étape 3 — compte DIRECTOR dans le tenant, avec son rôle.
            UUID directorId = createDirectorIfAbsent(schemaName, request);

            // Étape 4 — rattachement au compte multi-écoles (schema public).
            linkPersonToSchool(request.getEmail(), school, directorId, SystemRoleCodes.DIRECTOR);

        } catch (Exception e) {
            log.error("Onboarding incomplet pour '{}' (schema={}) : {}",
                    request.getSchoolName(), schemaName, e.getMessage(), e);

            markSchoolAsFailed(school.getId());

            throw new BusinessException(
                    "ONBOARDING_FAILED",
                    "L'inscription a échoué en cours de création. "
                            + "L'équipe technique a été notifiée ; réessayez dans quelques minutes."
            );
        }

        log.info("Onboarding terminé avec succès pour '{}' (schema={})",
                request.getSchoolName(), schemaName);

        return buildResponse(school);
    }

    public OnboardingResponse retryOnboarding(String slug) {
        School school = schoolRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(
                        "SCHOOL_NOT_FOUND", "École introuvable pour le slug : " + slug));

        String schemaName = school.getSchemaName();
        validateSchemaName(schemaName);

        log.info("Reprise de l'onboarding pour '{}' (schema={})", school.getName(), schemaName);

        try {
            provisionTenantSchema(schemaName);

            Optional<UUID> directorId = findUserIdByEmail(schemaName, school.getEmail());
            if (directorId.isEmpty()) {
                log.warn("Aucun directeur trouvé pour '{}' — un flux de création "
                        + "manuelle ou d'invitation est nécessaire.", school.getName());
            } else {
                linkPersonToSchool(school.getEmail(), school, directorId.get(), SystemRoleCodes.DIRECTOR);
            }

            reactivateSchool(school.getId());

        } catch (Exception e) {
            log.error("Échec de la reprise d'onboarding pour '{}' : {}",
                    school.getName(), e.getMessage(), e);
            throw new BusinessException("ONBOARDING_RETRY_FAILED",
                    "La reprise de l'inscription a échoué. Contactez le support.");
        }

        return buildResponse(school);
    }

    // =========================================================================
    // VALIDATIONS
    // =========================================================================

    private void validateUniqueness(String slug) {
        if (schoolRepository.existsBySlug(slug)) {
            throw BusinessException.conflict("SLUG_ALREADY_TAKEN", "Ce slug est déjà utilisé");
        }
        // L'email n'est PLUS vérifié ici : le même email peut légitimement
        // créer une DEUXIÈME école (portefeuille multi-écoles). schools.email
        // n'est d'ailleurs plus UNIQUE (voir V1__init_public_schema.sql) —
        // seule l'identité de la PERSONNE (persons.email) doit rester unique,
        // et Person.findByEmail réutilise simplement la ligne existante.
    }

    private SubscriptionPlan findPlanOrThrow(String planCode) {
        return planRepository.findByCode(planCode)
                .orElseThrow(() -> new BusinessException(
                        "PLAN_NOT_FOUND", "Plan de souscription introuvable : " + planCode));
    }

    private void validateSchemaName(String schemaName) {
        if (!SchemaNameValidator.isValid(schemaName)) {
            throw new BusinessException("INVALID_SCHEMA_NAME",
                    "Nom de schema invalide : " + schemaName);
        }
    }

    // =========================================================================
    // ÉTAPE 1 — École + abonnement
    // =========================================================================

    @Transactional
    public School createSchoolAndSubscription(
            OnboardingRequest request, SubscriptionPlan plan, String schemaName) {

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

        SchoolSubscription subscription = SchoolSubscription.builder()
                .school(school)
                .plan(plan)
                .billingCycle(BillingCycle.YEARLY)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(TRIAL_DURATION_DAYS))
                .status(SubscriptionStatus.ACTIVE)
                .autoRenew(true)
                .build();
        subscriptionRepository.save(subscription);

        return school;
    }

    // =========================================================================
    // ÉTAPE 2 — Provisioning du schema tenant
    // =========================================================================

    private void provisionTenantSchema(String schemaName) {
        log.debug("Création du schema : {}", schemaName);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        log.debug("Application des migrations Flyway sur : {}", schemaName);
        migrationService.migrateTenant(schemaName);
    }

    // =========================================================================
    // ÉTAPE 3 — Compte directeur (users + user_roles, insertion JDBC directe)
    // =========================================================================

    /**
     * Crée le directeur s'il n'existe pas déjà, avec son rôle DIRECTOR.
     *
     * CORRECTION — role_id (UUID) remplace role_code (String) suite à
     * l'introduction de la table public.roles. L'id du rôle DIRECTOR est
     * résolu UNE FOIS par requête JPA (schema public, indépendante du tenant),
     * puis inséré tel quel dans user_roles via JDBC — cohérent avec le reste
     * de cette méthode qui écrit déjà directement en SQL dans le tenant.
     */
    private UUID createDirectorIfAbsent(String schemaName, OnboardingRequest request) {
        Optional<UUID> existing = findUserIdByEmail(schemaName, request.getEmail());
        if (existing.isPresent()) {
            log.debug("Le directeur existe déjà pour {} — aucune création.", schemaName);
            return existing.get();
        }

        UUID directorId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO " + schemaName + ".users "
                        + "(id, first_name, last_name, email, password_hash, is_active) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                directorId,
                request.getDirectorFirstName(),
                request.getDirectorLastName(),
                request.getEmail(),
                passwordEncoder.encode(request.getDirectorPassword()),
                true
        );

        UUID directorRoleId = roleCatalogService.getIdByCode(SystemRoleCodes.DIRECTOR);

        jdbcTemplate.update(
                "INSERT INTO " + schemaName + ".user_roles (user_id, role_id) VALUES (?, ?)",
                directorId, directorRoleId
        );

        return directorId;
    }

    private Optional<UUID> findUserIdByEmail(String schemaName, String email) {
        List<UUID> ids = jdbcTemplate.query(
                "SELECT id FROM " + schemaName + ".users WHERE email = ?",
                (rs, rowNum) -> (UUID) rs.getObject("id"),
                email
        );
        return ids.stream().findFirst();
    }

    // =========================================================================
    // ÉTAPE 4 — Rattachement au compte multi-écoles (schema public)
    // =========================================================================

    /**
     * @param roleCode code du rôle (ex : SystemRoleCodes.DIRECTOR), stocké
     *                 tel quel dans SchoolMembership.rolesSnapshot — un simple
     *                 résumé d'affichage, pas une source de vérité pour les
     *                 autorisations (celles-ci viennent du JWT, alimenté par
     *                 user_roles côté tenant).
     */
    @Transactional
    public void linkPersonToSchool(String email, School school, UUID tenantUserId, String roleCode) {

        Person person = personRepository.findByEmail(email)
                .orElseGet(() -> personRepository.save(
                        Person.builder().email(email).build()));

        boolean alreadyMember = membershipRepository
                .findByPersonIdAndSchoolId(person.getId(), school.getId())
                .isPresent();

        if (!alreadyMember) {
            SchoolMembership membership = SchoolMembership.builder()
                    .person(person)
                    .school(school)
                    .tenantUserId(tenantUserId)
                    .rolesSnapshot(roleCode)
                    .isActive(true)
                    .build();
            membershipRepository.save(membership);
        }

        School schoolRef = entityManager.getReference(School.class, school.getId());
        person.setLastConnectedSchool(schoolRef);
        personRepository.save(person);
    }

    // =========================================================================
    // GESTION DES ÉCHECS
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSchoolAsFailed(UUID schoolId) {
        schoolRepository.findById(schoolId).ifPresent(school -> {
            school.setStatus(SchoolStatus.SUSPENDED);
            schoolRepository.save(school);
            log.warn("École {} marquée SUSPENDED suite à un échec de provisioning.", schoolId);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reactivateSchool(UUID schoolId) {
        schoolRepository.findById(schoolId).ifPresent(school -> {
            school.setStatus(SchoolStatus.TRIAL);
            schoolRepository.save(school);
            log.info("École {} réactivée (TRIAL) après reprise d'onboarding.", schoolId);
        });
    }

    // =========================================================================
    // CONSTRUCTION DE LA RÉPONSE
    // =========================================================================

    private OnboardingResponse buildResponse(School school) {
        return OnboardingResponse.builder()
                .tenantSlug(school.getSlug())
                .schemaName(school.getSchemaName())
                .schoolName(school.getName())
                .message("École créée. Connectez-vous avec votre email directeur.")
                .build();
    }
}
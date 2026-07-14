package com.schoolsaas.platform.service;

import org.springframework.stereotype.Service;
import com.schoolsaas.common.enums.BillingCycle;
import com.schoolsaas.common.enums.Role;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service responsable de l'onboarding des nouvelles écoles sur la plateforme
 * (F-01).
 *
 * -----------------------------------------------------------------------------
 * POURQUOI CE SERVICE N'EST PAS UN SIMPLE BLOC @Transactional
 * -----------------------------------------------------------------------------
 * Une tentation naturelle serait d'annoter onboard() avec @Transactional pour
 * obtenir un rollback automatique en cas d'échec. C'est un piège :
 *
 *   1. DEADLOCK GARANTI
 *      TenantMigrationService.migrateTenant() construit son propre objet
 *      Flyway, qui ouvre SA PROPRE connexion JDBC vers le DataSource — une
 *      connexion indépendante de celle que Spring attache au contexte
 *      @Transactional.
 *      Si le CREATE SCHEMA a été fait sur la connexion transactionnelle SANS
 *      être committé, il est invisible depuis la connexion de Flyway
 *      (isolation PostgreSQL). Flyway tente alors de verrouiller ce schema,
 *      se heurte au verrou tenu par la transaction ouverte, et ATTEND
 *      indéfiniment — pendant que cette même transaction attend le retour de
 *      Flyway pour continuer. Aucune des deux parties ne peut avancer.
 *
 *   2. LE DDL N'EST DE TOUTE FAÇON PAS TRANSACTIONNEL PARTOUT
 *      CREATE SCHEMA et les migrations Flyway ne peuvent pas être annulés par
 *      un simple ROLLBACK JPA : ce sont des opérations gérées par des
 *      connexions et des mécanismes distincts.
 *
 * CONSÉQUENCE : l'onboarding est découpé en étapes qui COMMITTENT
 * indépendamment. Il n'y a PAS de rollback global possible. En cas d'échec en
 * cours de route, l'école reste dans un état partiel — mais JAMAIS silencieux :
 * elle est marquée SUSPENDED (voir markSchoolAsFailed) et l'opération peut être
 * rejouée sans dupliquer ce qui a déjà réussi (voir idempotence ci-dessous).
 *
 * C'est un modèle de type Saga simplifié : pas d'atomicité globale, mais une
 * séquence d'étapes idempotentes avec un état observable à chaque étape.
 *
 * -----------------------------------------------------------------------------
 * IDEMPOTENCE — POURQUOI CHAQUE ÉTAPE PEUT ÊTRE REJOUÉE SANS DANGER
 * -----------------------------------------------------------------------------
 *   • CREATE SCHEMA IF NOT EXISTS   → sans effet si déjà créé
 *   • Flyway.migrate()              → ignore les migrations déjà appliquées
 *   • createDirectorIfAbsent()      → vérifie l'existence avant d'insérer
 *
 * Cela permet à retryOnboarding() de reprendre une inscription interrompue à
 * n'importe quelle étape, sans jamais créer de doublon.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final SchoolRepository             schoolRepository;
    private final SubscriptionPlanRepository   planRepository;
    private final SchoolSubscriptionRepository subscriptionRepository;
    private final TenantMigrationService       migrationService;
    private final PasswordEncoder              passwordEncoder;
    private final JdbcTemplate                 jdbcTemplate;

    /** Durée de la période d'essai (F-01). */
    private static final int TRIAL_DURATION_DAYS = 30;

    /**
     * Garde-fou anti-injection SQL. Le nom du schema est concaténé dans des
     * requêtes DDL (CREATE SCHEMA, INSERT ... INTO {schema}.users) car
     * PostgreSQL n'autorise pas les paramètres préparés pour les noms d'objets.
     * Doit rester aligné avec la contrainte chk_school_schema_name en base.
     */
    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    // =========================================================================
    // POINT D'ENTRÉE PRINCIPAL
    // =========================================================================

    /**
     * Inscrit une nouvelle école (F-01).
     *
     * Volontairement SANS @Transactional sur cette méthode : voir la Javadoc de
     * la classe. Chaque étape gère sa propre atomicité.
     *
     * @throws BusinessException SLUG_ALREADY_TAKEN, EMAIL_ALREADY_TAKEN,
     *                           PLAN_NOT_FOUND, ou ONBOARDING_FAILED si une
     *                           étape technique échoue après la création de
     *                           l'école (l'école est alors marquée SUSPENDED
     *                           et l'opération peut être rejouée via
     *                           retryOnboarding()).
     */
    public OnboardingResponse onboard(OnboardingRequest request) {
        log.info("Démarrage de l'onboarding pour l'école : {}", request.getSchoolName());

        validateUniqueness(request.getSlug(), request.getEmail());
        SubscriptionPlan plan = findPlanOrThrow(request.getPlanCode());
        String schemaName = SlugUtils.toSchemaName(request.getSlug());
        validateSchemaName(schemaName);

        // Étape 1 — écritures JPA (school + subscription), COMMITÉES avant de
        // passer au DDL. C'est ce commit qui évite le deadlock décrit plus haut.
        School school = createSchoolAndSubscription(request, plan, schemaName);

        // Étapes 2 et 3 — DDL et création du directeur. Si l'une échoue,
        // l'école est marquée en échec puis l'exception est propagée : le
        // contrôleur renvoie une erreur claire, et l'opération est rejouable.
        try {
            provisionTenantSchema(schemaName);
            createDirectorIfAbsent(schemaName, request);

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

    /**
     * Reprend un onboarding interrompu (école marquée SUSPENDED après un
     * ONBOARDING_FAILED).
     *
     * Chaque sous-étape est idempotente : rejouer cette méthode sur une école
     * déjà complètement provisionnée est sans danger (aucun doublon, aucune
     * erreur), ce qui permet de l'utiliser aussi comme simple vérification de
     * cohérence.
     */
    public OnboardingResponse retryOnboarding(String slug) {
        School school = schoolRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(
                        "SCHOOL_NOT_FOUND", "École introuvable pour le slug : " + slug));

        String schemaName = school.getSchemaName();
        validateSchemaName(schemaName);

        log.info("Reprise de l'onboarding pour '{}' (schema={})", school.getName(), schemaName);

        try {
            provisionTenantSchema(schemaName);

            // Il n'y a pas de mot de passe à disposition lors d'une reprise :
            // si le directeur n'existe pas encore, il faut le recréer via un
            // flux dédié (ex : lien d'activation envoyé par email), pas ici.
            if (!directorExists(schemaName, school.getEmail())) {
                log.warn("Aucun directeur trouvé pour '{}' — un flux de création "
                        + "manuelle ou d'invitation est nécessaire.", school.getName());
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

    private void validateUniqueness(String slug, String email) {
        if (schoolRepository.existsBySlug(slug)) {
            throw BusinessException.conflict("SLUG_ALREADY_TAKEN", "Ce slug est déjà utilisé");
        }
        if (schoolRepository.existsByEmail(email)) {
            throw BusinessException.conflict("EMAIL_ALREADY_TAKEN", "Cet email est déjà utilisé");
        }
    }

    private SubscriptionPlan findPlanOrThrow(String planCode) {
        return planRepository.findByCode(planCode)
                .orElseThrow(() -> new BusinessException(
                        "PLAN_NOT_FOUND", "Plan de souscription introuvable : " + planCode));
    }

    /**
     * Revalide le nom de schema calculé par SlugUtils. Double sécurité : la
     * contrainte chk_school_schema_name en base rejetterait de toute façon un
     * nom invalide, mais échouer ici évite un aller-retour SQL inutile et
     * produit un message d'erreur explicite plutôt qu'une DataIntegrityViolation.
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !SCHEMA_NAME_PATTERN.matcher(schemaName).matches()) {
            throw new BusinessException("INVALID_SCHEMA_NAME",
                    "Nom de schema invalide : " + schemaName);
        }
    }

    // =========================================================================
    // ÉTAPE 1 — École + abonnement (transaction indépendante et committée)
    // =========================================================================

    /**
     * Crée l'école et son abonnement dans UNE transaction dédiée.
     *
     * @Transactional ici (et non sur onboard()) est essentiel : à la sortie de
     * cette méthode, la transaction est committée et le schema — créé à l'étape
     * suivante — sera visible depuis la connexion indépendante de Flyway.
     *
     * Propagation par défaut (REQUIRED) : suffisant ici car cette méthode est
     * appelée depuis onboard(), qui n'ouvre elle-même aucune transaction. Le
     * commit a donc bien lieu au retour de cette méthode.
     */
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
    // ÉTAPE 2 — Provisioning du schema tenant (DDL, hors transaction JPA)
    // =========================================================================

    /**
     * Crée le schema PostgreSQL de l'école puis y applique les migrations Flyway.
     *
     * IF NOT EXISTS rend cette étape idempotente : rejouable après un échec
     * précédent sans provoquer d'erreur si le schema existe déjà.
     */
    private void provisionTenantSchema(String schemaName) {
        log.debug("Création du schema : {}", schemaName);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        log.debug("Application des migrations Flyway sur : {}", schemaName);
        migrationService.migrateTenant(schemaName);
    }

    // =========================================================================
    // ÉTAPE 3 — Création du compte directeur
    // =========================================================================

    /**
     * Crée le compte DIRECTOR dans le schema tenant, sauf s'il existe déjà.
     *
     * La vérification préalable rend cette étape idempotente : une reprise
     * après échec ne provoque pas de violation de contrainte UNIQUE sur l'email.
     */
    private void createDirectorIfAbsent(String schemaName, OnboardingRequest request) {
        if (directorExists(schemaName, request.getEmail())) {
            log.debug("Le directeur existe déjà pour {} — aucune action.", schemaName);
            return;
        }

        String sql = "INSERT INTO " + schemaName + ".users "
                + "(first_name, last_name, email, password_hash, role, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                request.getDirectorFirstName(),
                request.getDirectorLastName(),
                request.getEmail(),
                passwordEncoder.encode(request.getDirectorPassword()),
                Role.DIRECTOR.name(),
                true
        );
    }

    /**
     * Vérifie l'existence d'un utilisateur par email dans le schema tenant.
     *
     * Requête SQL native et non JPA : le schema tenant n'est pas connu au
     * moment de la compilation, et cette vérification intervient avant même que
     * le TenantContext ne soit positionné (ce code s'exécute côté onboarding,
     * dans le schema public, pas dans le contexte d'une requête utilisateur
     * authentifiée). Le nom de schema est validé en amont (validateSchemaName),
     * la concaténation est donc sûre.
     */
    private boolean directorExists(String schemaName, String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + schemaName + ".users WHERE email = ?",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    // =========================================================================
    // GESTION DES ÉCHECS
    // =========================================================================

    /**
     * Marque une école en échec de provisioning.
     *
     * Propagation.REQUIRES_NEW est OBLIGATOIRE ici, et non un simple détail
     * d'optimisation.
     *
     * Cette méthode est appelée depuis le bloc catch de onboard(), qui lève
     * ensuite une BusinessException. Sans REQUIRES_NEW, si onboard() venait un
     * jour à être appelée depuis un contexte lui-même @Transactional (par
     * exemple un contrôleur ou un test annoté @Transactional), l'UPDATE
     * ci-dessous ne serait qu'un savepoint interne à cette transaction
     * englobante — et le throw qui suit dans onboard() en provoquerait le
     * ROLLBACK, y compris de CE marquage. L'école resterait alors TRIAL,
     * silencieusement cassée, sans aucune trace de l'échec.
     *
     * REQUIRES_NEW suspend la transaction courante (s'il y en a une), ouvre une
     * transaction entièrement nouvelle, la committe à son retour — rendant ce
     * marquage acquis quoi qu'il arrive ensuite dans l'appelant.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSchoolAsFailed(UUID schoolId) {
        schoolRepository.findById(schoolId).ifPresent(school -> {
            school.setStatus(SchoolStatus.SUSPENDED);
            schoolRepository.save(school);
            log.warn("École {} marquée SUSPENDED suite à un échec de provisioning.", schoolId);
        });
    }

    /**
     * Réactive une école après une reprise d'onboarding réussie.
     * Même raisonnement que markSchoolAsFailed : transaction indépendante.
     */
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
package com.schoolsaas.identity.service;

import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.config.multitenancy.TenantContext;
import com.schoolsaas.identity.dto.request.LoginRequest;
import com.schoolsaas.identity.dto.response.AuthResponse;
import com.schoolsaas.identity.dto.response.SchoolSummaryResponse;
import com.schoolsaas.platform.entity.Person;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.entity.SchoolMembership;
import com.schoolsaas.platform.repository.PersonRepository;
import com.schoolsaas.platform.repository.SchoolMembershipRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Authentification multi-écoles (F-02).
 *
 * -----------------------------------------------------------------------------
 * PAS DE @Transactional GLOBAL — même raison que pour les schedulers et
 * l'onboarding : Hibernate fige le tenant résolu par TenantIdentifierResolver
 * à la CRÉATION de la session, pas à chaque requête. Mélanger un accès schema
 * public (Person, School) et un accès schema tenant (User) dans une seule
 * transaction utilise le mauvais schema pour le second accès. Principe
 * conservé : orchestrateur SANS transaction, qui positionne TenantContext
 * puis délègue à un bean EXTERNE (AuthTenantService) dont la méthode est
 * @Transactional — l'appel passe par le proxy Spring, une session neuve
 * s'ouvre avec le tenant déjà correctement positionné.
 * -----------------------------------------------------------------------------
 * RÉSOLUTION DE L'ÉCOLE
 * -----------------------------------------------------------------------------
 * Le flux ne demande plus l'école à l'avance : la personne tape seulement
 * son email et son mot de passe. L'école cible est résolue AVANT toute
 * vérification de mot de passe, via resolveTargetMembership(), dans cet
 * ordre :
 *   1. tenantSlug fourni explicitement (option avancée)     → cette école
 *   2. Person.lastConnectedSchool, si encore active         → cette école
 *   3. Aucune des deux : choix ALÉATOIRE parmi les écoles
 *      actives restantes de la personne                     → repli
 *   4. Aucune école active du tout                           → accès refusé
 *
 * Une SEULE vérification de mot de passe par connexion (coût constant, quel
 * que soit le nombre d'écoles possédées). Le même algorithme couvre le cas
 * "école habituelle suspendue et aucune autre" sans code particulier :
 * l'étape 3 ne trouve rien, l'étape 4 bloque l'accès.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonRepository           personRepository;
    private final SchoolMembershipRepository membershipRepository;
    private final AuthTenantService          authTenantService;
    private final EntityManager              entityManager;

    /** Authentifie une personne et résout automatiquement son école (F-02a). */
    public AuthResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour {}", request.getEmail());

        Person person = personRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.unauthorized(
                        "INVALID_CREDENTIALS", "Identifiants incorrects"));

        SchoolMembership target = resolveTargetMembership(person, request.getTenantSlug());
        School school = target.getSchool();

        boolean fallbackOccurred = person.getLastConnectedSchool() != null
                && !person.getLastConnectedSchool().getId().equals(school.getId());

        try {
            TenantContext.set(school.getSchemaName());

            AuthResponse response = authTenantService.authenticate(request, school, fallbackOccurred);
            updateLastConnectedSchool(person.getId(), school.getId());
            return response;

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Change d'école SANS reconnexion, pour une personne déjà authentifiée.
     * Ne revérifie AUCUN mot de passe : l'identité a déjà été prouvée par le
     * token d'accès courant. Seule l'appartenance active est contrôlée.
     *
     * @param currentEmail email extrait du token d'accès courant (SecurityContext)
     */
    public AuthResponse switchSchool(String currentEmail, String targetSlug) {
        Person person = personRepository.findByEmail(currentEmail)
                .orElseThrow(() -> BusinessException.unauthorized(
                        "INVALID_TOKEN", "Compte introuvable"));

        SchoolMembership target = membershipRepository
                .findActiveOperationalByPersonId(person.getId())
                .stream()
                .filter(m -> m.getSchool().getSlug().equals(targetSlug))
                .findFirst()
                .orElseThrow(() -> BusinessException.unauthorized(
                        "SCHOOL_NOT_ASSOCIATED",
                        "Ce compte n'est pas associé à cette école, ou elle est indisponible"));

        School school = target.getSchool();

        try {
            TenantContext.set(school.getSchemaName());

            // Réutilise refreshTokens() : même besoin (charger un utilisateur
            // par id, vérifier isActive, émettre des tokens), sans mot de passe.
            AuthResponse response = authTenantService.refreshTokens(
                    target.getTenantUserId(), school.getSchemaName());

            updateLastConnectedSchool(person.getId(), school.getId());
            return response;

        } finally {
            TenantContext.clear();
        }
    }

    /** Liste les écoles actives de la personne authentifiée (sélecteur d'écoles). */
    public List<SchoolSummaryResponse> getMySchools(String currentEmail) {
        Person person = personRepository.findByEmail(currentEmail)
                .orElseThrow(() -> BusinessException.unauthorized(
                        "INVALID_TOKEN", "Compte introuvable"));

        return membershipRepository.findActiveOperationalByPersonId(person.getId())
                .stream()
                .map(m -> new SchoolSummaryResponse(
                        m.getSchool().getSlug(),
                        m.getSchool().getName(),
                        m.getRolesSnapshot()))
                .toList();
    }

    /**
     * Renouvelle les tokens à partir d'un refresh token valide (F-02b).
     * Revalide systématiquement l'utilisateur en base — jamais de confiance
     * aveugle dans les claims du vieux token.
     */
    public AuthResponse refresh(String refreshToken) {
        if (!authTenantService.isTokenValid(refreshToken)) {
            throw BusinessException.unauthorized(
                    "INVALID_TOKEN", "Token de rafraîchissement invalide ou expiré");
        }
        if (!authTenantService.isRefreshTokenType(refreshToken)) {
            throw BusinessException.unauthorized(
                    "INVALID_TOKEN_TYPE", "Ce token n'est pas un token de rafraîchissement");
        }

        String tenantSchema = authTenantService.extractTenantSchema(refreshToken);
        UUID   userId        = authTenantService.extractUserId(refreshToken);

        try {
            TenantContext.set(tenantSchema);
            return authTenantService.refreshTokens(userId, tenantSchema);
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // Résolution de l'école cible
    // =========================================================================

    private SchoolMembership resolveTargetMembership(Person person, String explicitSlug) {

        List<SchoolMembership> activeMemberships =
                membershipRepository.findActiveOperationalByPersonId(person.getId());

        if (activeMemberships.isEmpty()) {
            throw BusinessException.unauthorized(
                    "NO_ACTIVE_SCHOOL", "Aucune école active n'est associée à ce compte");
        }

        if (explicitSlug != null && !explicitSlug.isBlank()) {
            return activeMemberships.stream()
                    .filter(m -> m.getSchool().getSlug().equals(explicitSlug))
                    .findFirst()
                    .orElseThrow(() -> BusinessException.unauthorized(
                            "SCHOOL_NOT_ASSOCIATED",
                            "Ce compte n'est pas associé à cette école, ou elle est indisponible"));
        }

        UUID lastSchoolId = person.getLastConnectedSchool() != null
                ? person.getLastConnectedSchool().getId()
                : null;

        if (lastSchoolId != null) {
            Optional<SchoolMembership> usual = activeMemberships.stream()
                    .filter(m -> m.getSchool().getId().equals(lastSchoolId))
                    .findFirst();
            if (usual.isPresent()) {
                return usual.get();
            }
        }

        // Repli aléatoire. Si une seule école active reste, elle est
        // mécaniquement choisie.
        SchoolMembership fallback = activeMemberships.get(
                ThreadLocalRandom.current().nextInt(activeMemberships.size()));

        log.warn("École habituelle indisponible pour {} — repli sur {}",
                person.getEmail(), fallback.getSchool().getSlug());

        return fallback;
    }

    /**
     * Met à jour la dernière école visitée.
     * entityManager.getReference() : proxy Hibernate portant seulement l'id,
     * sans déclencher de SELECT.
     * Propagation.REQUIRES_NEW : commit indépendant, cohérent avec
     * OnboardingService.markSchoolAsFailed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLastConnectedSchool(UUID personId, UUID schoolId) {
        personRepository.findById(personId).ifPresent(person -> {
            School reference = entityManager.getReference(School.class, schoolId);
            person.setLastConnectedSchool(reference);
            personRepository.save(person);
        });
    }
}
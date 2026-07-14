package com.schoolsaas.identity.service;

import com.schoolsaas.common.enums.SchoolStatus;
import com.schoolsaas.common.exception.BusinessException;
import com.schoolsaas.config.multitenancy.TenantContext;
import com.schoolsaas.identity.dto.request.LoginRequest;
import com.schoolsaas.identity.dto.response.AuthResponse;
import com.schoolsaas.platform.entity.School;
import com.schoolsaas.platform.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Authentification (F-02).
 *
 * -----------------------------------------------------------------------------
 * POURQUOI CETTE CLASSE N'A PLUS DE @Transactional
 * -----------------------------------------------------------------------------
 * La version précédente portait @Transactional sur login() en entier, avec ce
 * déroulé :
 *     1. schoolRepository.findBySlugAndStatusIn(...)   ← lecture en schema public
 *     2. TenantContext.set(school.getSchemaName())
 *     3. userRepository.findByEmailAndIsActiveTrue(...) ← lecture en schema tenant
 *
 * C'est EXACTEMENT le bug déjà rencontré sur les schedulers (relation
 * "student_fees" does not exist) : @Transactional lie une session Hibernate à
 * la transaction DÈS L'ENTRÉE dans la méthode. Le tenant résolu par
 * TenantIdentifierResolver est figé à CE moment-là — pas à chaque requête.
 *
 * Au moment où la transaction s'ouvre, TenantContext.get() vaut encore null
 * (rien n'a encore été positionné) → le tenant se fige sur "public" pour toute
 * la durée de cette session. L'étape 1 fonctionne par coïncidence, car School
 * porte @Table(schema = "public") — la requête est qualifiée explicitement,
 * indifférente au search_path. Mais User n'a AUCUNE qualification de schema :
 * elle dépend entièrement du search_path, déjà figé sur "public". Le
 * TenantContext.set() de l'étape 2 arrive trop tard pour la session déjà ouverte.
 *
 * → Symptôme attendu en production : "relation users does not exist".
 *
 * CORRECTION : le principe déjà appliqué aux schedulers. L'orchestrateur (cette
 * classe) N'OUVRE AUCUNE TRANSACTION. Il charge l'école (accès public, sans
 * dépendance au tenant), positionne TenantContext, PUIS délègue à un bean
 * EXTERNE (AuthTenantService) dont la méthode est annotée @Transactional.
 * L'appel passe par le proxy Spring : une session Hibernate NEUVE est ouverte à
 * cet instant, avec le tenant déjà correctement positionné dès sa création.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SchoolRepository  schoolRepository;
    private final AuthTenantService authTenantService;

    /**
     * Authentifie un utilisateur pour une école donnée (F-02a).
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour {} sur le tenant {}",
                request.getEmail(), request.getTenantSlug());

        // Lecture en schema PUBLIC — indépendante du TenantContext puisque
        // School est explicitement qualifiée (@Table(schema = "public")).
        // Peut donc être faite AVANT tout positionnement du tenant.
        School school = schoolRepository
                .findBySlugAndStatusIn(request.getTenantSlug(),
                        List.of(SchoolStatus.ACTIVE, SchoolStatus.TRIAL))
                .orElseThrow(() -> BusinessException.notFound(
                        "TENANT_NOT_FOUND", "École inactive ou inexistante"));

        try {
            TenantContext.set(school.getSchemaName());

            // Appel EXTERNE (bean différent) → passe par le proxy Spring →
            // une transaction (et donc une session Hibernate) neuve s'ouvre
            // ICI, avec le tenant déjà positionné.
            return authTenantService.authenticate(request, school);

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Renouvelle les tokens à partir d'un refresh token valide (F-02b).
     *
     * CORRECTION DE SÉCURITÉ MAJEURE vs la version précédente :
     *
     *   L'ancienne implémentation régénérait des tokens en lisant SEULEMENT
     *   les claims du VIEUX refresh token (email, tenantId, roles), SANS
     *   JAMAIS interroger la base de données. Conséquences concrètes :
     *
     *     • Un utilisateur désactivé (isActive = false) après l'émission du
     *       token continuait d'obtenir des tokens valides indéfiniment, tant
     *       qu'il rafraîchissait dans la fenêtre de 7 jours.
     *     • Un rôle rétrogradé par le directeur (TEACHER → aucun droit) restait
     *       actif dans les nouveaux tokens émis, car les rôles étaient recopiés
     *       tels quels depuis l'ancien token, jamais relus depuis la table users.
     *     • Une école SUSPENDUE pour impayé (SchoolStatus.SUSPENDED) continuait
     *       de fonctionner indéfiniment via refresh — la suspension n'était
     *       vérifiée qu'au login initial, jamais ensuite. Pour un SaaS dont la
     *       suspension pour impayé est un mécanisme central, c'est un trou
     *       direct dans le modèle économique.
     *
     *   CORRECTION : refresh() revalide désormais systématiquement l'état réel
     *   de l'école ET de l'utilisateur en base, exactement comme le ferait un
     *   nouveau login — via le même mécanisme de délégation à un bean externe.
     *
     * CORRECTION DE SÉCURITÉ SECONDAIRE — confusion de type de token :
     *   Rien ne vérifiait que le token soumis ici était bien un refresh token
     *   (claim "type" = "refresh") et non un access token. Un access token
     *   (valide 1h) aurait pu être présenté à cet endpoint pour obtenir de
     *   nouveaux tokens indéfiniment, contournant sa durée de vie prévue.
     */
    public AuthResponse refresh(String refreshToken) {
        validateRefreshToken(refreshToken);

        String tenantSchema = authTenantService.extractTenantSchema(refreshToken);
        UUID   userId        = authTenantService.extractUserId(refreshToken);

        // Vérification de l'école en schema PUBLIC, avant tout positionnement
        // du tenant — mêmes raisons qu'en login() : School est schema-qualifiée,
        // aucune dépendance au TenantContext ici.
        schoolRepository.findBySchemaNameAndStatusIn(tenantSchema,
                        List.of(SchoolStatus.ACTIVE, SchoolStatus.TRIAL))
                .orElseThrow(() -> BusinessException.unauthorized(
                        "TENANT_SUSPENDED", "École suspendue ou inexistante"));

        try {
            TenantContext.set(tenantSchema);

            // Appel externe → nouvelle session Hibernate, tenant déjà positionné.
            return authTenantService.refreshTokens(userId, tenantSchema);

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Valide la signature/expiration du token ET son TYPE.
     * Sans cette seconde vérification, un access token pourrait être utilisé
     * comme refresh token (voir Javadoc de refresh() ci-dessus).
     */
    private void validateRefreshToken(String token) {
        if (!authTenantService.isTokenValid(token)) {
            throw BusinessException.unauthorized(
                    "INVALID_TOKEN", "Token de rafraîchissement invalide ou expiré");
        }
        if (!authTenantService.isRefreshTokenType(token)) {
            throw BusinessException.unauthorized(
                    "INVALID_TOKEN_TYPE", "Ce token n'est pas un token de rafraîchissement");
        }
    }
}
# Analyse détaillée du projet — SchoolSaaS Backend

> Rapport généré le 2026-07-15 par analyse directe du code source (`school-management-service`, backend Java/Spring Boot d'un monorepo qui contient aussi `school-management-client`, l'app Angular).

---

## 1. Vue d'ensemble

**SchoolSaaS** est un ERP multi-tenant destiné aux écoles privées en Guinée / Afrique de l'Ouest. Ce dépôt contient uniquement le **backend** (API REST). Le dépôt parent (`school-management/`, racine git réelle) contient aussi `school-management-client` (Angular), qui consomme des types TypeScript générés automatiquement depuis les DTOs Java au build (`typescript-generator-maven-plugin`, voir §7).

| | |
|---|---|
| Langage / Runtime | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Base de données | PostgreSQL 16, schema-per-tenant |
| Sécurité | JWT stateless, Spring Security 6, BCrypt (force 12) |
| Migrations | Flyway (3 jeux de scripts : public / tenant / seed) |
| Mapping DTO | MapStruct 1.6.2 |
| PDF | Thymeleaf + OpenHTMLtoPDF |
| Cache | Caffeine |
| Doc API | springdoc-openapi 2.6.0 (Swagger UI) |
| Tests | JUnit 5, Mockito, Testcontainers, H2, Jacoco |
| Fichiers Java (main) | ~225 |
| Fichiers de test | 33 |

---

## 2. Architecture multi-tenant (le cœur du système)

Le modèle est **schema-per-tenant** : chaque école a son propre schéma PostgreSQL (ex. `tenant_ste_marie`), isolant physiquement ses données (élèves, notes, paiements). Un schéma `public` unique porte les données de la plateforme elle-même (écoles, abonnements, catalogue de rôles).

### 2.1 Résolution du tenant à chaque requête

1. `JwtAuthenticationFilter` (`config/security/JwtAuthenticationFilter.java`) intercepte chaque requête, extrait le JWT du header `Authorization: Bearer`, en tire `tenantId`, `userId`, `roles`.
2. Il pose le tenant courant dans un `ThreadLocal` via `TenantContext.set(tenantId)` (`config/multitenancy/TenantContext.java`), et l'ajoute au MDC de logging (`%X{tenantId}` — chaque ligne de log affiche l'école concernée).
3. En fin de requête, le `finally` du filtre appelle systématiquement `TenantContext.clear()` — évite la fuite d'un tenant vers la requête suivante sur un thread réutilisé (pool Tomcat).

### 2.2 Branchement Hibernate

- `TenantIdentifierResolver` (`CurrentTenantIdentifierResolver<String>`) lit `TenantContext.get()` et retombe sur `"public"` si absent.
- `SchemaMultiTenantConnectionProvider` exécute `SET search_path = <tenant>, public` sur la connexion JDBC obtenue, et la remet à `public` en la relâchant. Le nom de schéma est validé par regex (`^[a-zA-Z0-9_]+$`) avant d'être concaténé dans le SQL — protection anti-injection nécessaire car PostgreSQL n'autorise pas les requêtes préparées sur les identifiants d'objets (`SET search_path`).
- **Point notable** : `MultiTenancyConfig` (un `HibernatePropertiesCustomizer`) existe spécifiquement pour enregistrer les **instances Spring** de ces deux beans auprès d'Hibernate. Un commentaire dans le code documente un bug de prod déjà rencontré : déclarer les classes en `@Component` ne suffit pas à les faire utiliser par Hibernate 6 si on ne les câble pas explicitement — sinon Hibernate tourne en mono-tenant silencieux sur `public`, avec des erreurs `relation "users" does not exist` en aval.

### 2.3 Onboarding et migration d'un tenant

- `OnboardingService` (`platform/service/OnboardingService.java`) crée l'école (schéma `public.schools`), crée le schéma PostgreSQL, puis délègue à `TenantMigrationService.migrateTenant(schemaName)` (`platform/service/TenantMigrationService.java`) qui lance Flyway avec `locations=classpath:db/migration/tenant`, `baselineVersion("0")` et un `flyway.repair()` préventif avant `migrate()` (corrige un cas réel où une migration V1 échouée bloquait les tentatives suivantes).
- `platform/config/TenantInitializer.java` (`CommandLineRunner`) fait le même travail **au démarrage de l'application** : il récupère toutes les écoles `TRIAL`/`ACTIVE` (`SchoolRepository.findAllByStatusIn`) et applique `V1__init_tenant_schema.sql` sur chacune — garantit que tous les tenants existants restent à jour après un déploiement, sans attendre un nouvel onboarding.
- Le script `db/migration/seed/V2__insert_sample_school.sql` insère une école de démo (« Lycée Sainte Marie de Dixinn ») + le catalogue tarifaire (3 plans : Starter/Standard/Premium, en GNF). **Ce seed n'est chargé qu'en profil dev** (`application-dev.yml` : `spring.flyway.locations: classpath:db/migration/public,classpath:db/migration/seed`) — absent des locations Flyway de `application-prod.yml`, qui ne charge que `classpath:db/migration/public`.

### 2.4 Identité transverse multi-écoles

Le schéma `public` porte deux tables ajoutées pour un besoin produit précis : une même personne (propriétaire de plusieurs écoles, enseignant multi-établissements) doit pouvoir se connecter une fois et changer d'école sans se reconnecter :
- `persons` (email global, **sans mot de passe** — volontaire, voir §5) ;
- `school_memberships` (lien personne ↔ école ↔ `tenant_user_id`, avec `roles_snapshot` dénormalisé pour l'affichage, mais **jamais utilisé pour l'autorisation réelle**, qui repose sur le JWT émis après résolution du tenant).

Cette logique est implémentée côté service dans `identity/service/AuthTenantService.java` et `identity/service/AuthService.java` (login, switch-school), avec les entités `platform/entity/Person.java`, `platform/entity/SchoolMembership.java`, `platform/entity/Role.java` (catalogue de rôles en base, plus figé en enum Java — voir migration §5).

---

## 3. Sécurité

- **`SecurityConfig`** (`config/security/SecurityConfig.java`) : CSRF désactivé (API stateless), session `STATELESS`, `@EnableMethodSecurity` actif (autorise `@PreAuthorize` au niveau méthode).
- Endpoints publics explicites : `/api/v1/auth/**`, `/api/v1/platform/onboard`, Swagger/OpenAPI, `/actuator/**`. Tout le reste exige une authentification (`anyRequest().authenticated()`).
- **JWT** : `JwtService` gère génération/validation ; le token porte `userId`, `tenantId`, `roles` en claims. Expiration access token 1h, refresh token 7j (`application.yml`).
- **Rôles** : le catalogue (`DIRECTOR`, `TEACHER`, `ACCOUNTANT`, etc.) a été migré d'un `enum` Java fixe (`common/enums/Role.java`, encore présent mais partiellement remplacé) vers une **table `public.roles`** — objectif explicite documenté en SQL : permettre l'ajout d'un rôle (« INFIRMIER ») via une future API d'admin, sans nouvelle migration ni redéploiement. La suppression d'un rôle est modélisée en désactivation (`is_active=false`), jamais un `DELETE`, pour ne pas casser les FK inter-schémas existantes dans chaque tenant.
- **Mot de passe** : BCrypt, force 12 (`SecurityConfig.passwordEncoder()`).
- **Autorisation par méthode** : `@PreAuthorize` est utilisé dans 15 contrôleurs (academic, class, subject, attendance, sms, dashboard, enrollment, promotion, student, fee-structure, payment, grade, report-card, user, timetable). Expressions observées : `isAuthenticated()`, `hasRole('DIRECTOR')`, `hasAnyRole('DIRECTOR','TEACHER')`, `hasAnyRole('DIRECTOR','ACCOUNTANT')`, `hasAnyRole('DIRECTOR','TEACHER','ACCOUNTANT')`. `DIRECTOR` est systématiquement le rôle le plus permissif. **`PARENT` n'apparaît dans aucune annotation `@PreAuthorize`** — aucun endpoint n'est aujourd'hui explicitement réservé à un accès parent, alors que la constante existe (`SystemRoleCodes.java`) et que le produit vise la communication avec les parents.
- **✅ CORS — corrigé depuis la rédaction de ce rapport** : `SecurityConfig.corsConfigurationSource()` avait `allowedOrigins(List.of("*"))` codé en dur avec le commentaire `// Change this for production`, alors qu'`application-prod.yml` définissait bien une clé `app.cors.allowed-origins: - ${FRONTEND_URL}` jamais lue par le code Java. Corrigé : une classe `CorsProperties` (`@ConfigurationProperties(prefix = "app.cors")`) a été introduite pour que le bean CORS lise réellement cette valeur (commit `c9c3b69`).
- **Header `X-Tenant-Id`** : autorisé côté CORS (`SecurityConfig.java`) mais **jamais lu** côté Java — le tenant est résolu exclusivement via le claim `tenantId` du JWT (§2.1). Probablement un vestige ou une préparation pour un usage futur (ex. sélection de tenant avant authentification).
- **Actuator** : `/actuator/**` est `permitAll()` dans `SecurityConfig` **quel que soit le profil**. La restriction réelle en prod vient uniquement de `management.endpoints.web.exposure.include: health` (application-prod.yml) qui limite les endpoints *exposés*, pas d'une règle Spring Security différenciée — en dev, `include: "*"` expose donc tous les endpoints Actuator sans authentification.

---

## 4. Organisation modulaire (package par domaine)

Chaque module suit la même structure interne : `controller/`, `dto/{request,response}/`, `entity/`, `mapper/` (MapStruct), `repository/` (Spring Data JPA), `service/`.

| Module | Fichiers | Entités principales | Endpoints REST (base path) | Rôle métier |
|---|---|---|---|---|
| `platform` | 21 | `School`, `SubscriptionPlan`, `SchoolSubscription`, `SubscriptionPayment`, `Person`, `SchoolMembership`, `Role` | `/api/v1/platform/onboard` | Gestion des tenants (écoles) et de leur abonnement SaaS ; onboarding, facturation plateforme |
| `identity` | 18 | `User`, `Teacher`, `UserRoleAssignment` | `/api/v1/auth`, `/api/v1/school/users` | Authentification (login, switch-school), gestion des comptes utilisateurs et rôles par école |
| `academic` | 18 | `AcademicYear`, `Term`, `Level`, `SchoolClass`, `Subject`, `ClassSubject` | `/api/v1/school` (années, classes, matières) | Structure pédagogique : années scolaires, cycles, classes, matières |
| `enrollment` | 19 | `Student`, `StudentEnrollment`, `PromotionBatch` | `/api/v1/school` (étudiants), `/api/v1/school/promotion-batches` | Inscription des élèves, promotion de fin d'année |
| `finance` | 19 | `FeeStructure`, `StudentFee`, `Payment`, `PaymentAllocation` | `/api/v1/school` (frais/paiements) | Structures de frais, paiements des parents, allocation des paiements aux frais dus |
| `grading` | 15 | `Grade`, `ReportCard` | `/api/v1/school/grades`, `/api/v1/school/report-cards` | Saisie des notes, génération de bulletins |
| `attendance` | 9 | `Attendance` | `/api/v1/school/attendance` | Présences/absences |
| `timetable` | 9 | `TimeSlot`, `TimetableEntry` | `/api/v1/school` (emploi du temps) | Emploi du temps hebdomadaire |
| `communication` | 10 | `SmsTemplate`, `SmsLog` | `/api/v1/school/sms` | Envoi de SMS (rappels de frais, notifications de notes), templates |
| `dashboard` | 4 | — (agrégations, pas d'entité propre) | `/api/v1/school/dashboard` | KPIs et tableaux de bord (mis en cache Caffeine, rafraîchis par scheduler) |
| `infrastructure` | 7 | — | — | Services transversaux : SMS, stockage, génération PDF (voir §6) |
| `common` | — | `BaseEntity`, `GenderConverter` | — | Base entity (id/audit), enums partagés, exceptions métier, constantes |

Note : plusieurs modules exposent leurs endpoints sous le préfixe générique `/api/v1/school` sans sous-chemin dédié dans certains contrôleurs (`EnrollmentController`, `AcademicYearController`, `ClassController`, `PaymentController`, `FeeStructureController`, `TimetableController`) — la distinction se fait alors au niveau des méthodes (`@GetMapping("/students")`, etc.), pas du `@RequestMapping` de classe.

### Schedulers (tâches planifiées)

Trois schedulers, configurés en cron dans `application.yml` :
- `dashboard/scheduler/DashboardScheduler.java` — rafraîchit le cache dashboard toutes les 15 min.
- `communication/scheduler/SmsScheduler.java` — rappels de frais (8h, lun-ven).
- `platform/scheduler/SubscriptionScheduler.java` — traitement des abonnements arrivant à échéance / bascule `OVERDUE` (minuit).

Le fuseau horaire est porté par `schools.timezone` (donnée métier, pas un réglage technique) : les schedulers tournent en UTC et filtrent par école pour déclencher l'action à l'heure locale voulue (ex. Conakry vs Abidjan avec un seul scheduler).

---

## 5. Base de données (migrations Flyway)

Trois jeux de scripts, avec un commentaire explicite dans le SQL sur la distinction des deux flux d'argent à ne pas confondre :
- **Public** : l'**école** paie **SchoolSaaS** (abonnement) → `subscription_payments`.
- **Tenant** : le **parent** paie l'**école** (frais de scolarité) → `payments` / `payment_allocations`.

### 5.1 `db/migration/public/V1__init_public_schema.sql` (504 lignes)

Tables : `roles`, `subscription_plans`, `schools`, `school_subscriptions`, `subscription_payments`, `persons`, `school_memberships`.

Points de conception notables (documentés en commentaires SQL, signe d'un historique de corrections réelles) :
- Contrainte `chk_plan_yearly_discount` : le prix annuel doit rester ≤ 12× le prix mensuel (incitation commerciale cohérente).
- Index unique **partiel** `uq_school_active_subscription` : une école ne peut avoir qu'un seul abonnement `ACTIVE` à la fois (corrige un bug de double-abonnement lors d'un renouvellement interrompu).
- `schools.email` volontairement **non unique** (contrairement à une version précédente) : un même propriétaire peut avoir plusieurs écoles avec le même email de contact.
- `schools.schema_name` contraint par regex + `VARCHAR(63)` (limite dure PostgreSQL pour un identifiant, au-delà de laquelle le nom serait tronqué silencieusement).
- Tous les enums métier sont modélisés en `VARCHAR + CHECK` (pas de type `ENUM` Postgres), en **MAJUSCULES**, pour matcher l'écriture Java de `@Enumerated(EnumType.STRING)` — un bug antérieur avait des valeurs en minuscules (`'active'`) rejetées par la contrainte dès que l'entité Java est passée de `String` à un enum.
- Triggers `updated_at` ajoutés a posteriori sur les 7 tables (absents du script d'origine, la colonne existait mais n'était jamais mise à jour).

### 5.2 `db/migration/tenant/V1__init_tenant_schema.sql` (1450 lignes, 22 tables)

`users`, `user_roles`, `teachers`, `academic_years`, `terms`, `levels`, `classes`, `subjects`, `class_subjects`, `students`, `enrollments`, `promotion_batches`, `fee_structures`, `student_fees`, `payments`, `payment_allocations`, `sms_templates`, `sms_logs`, `grades`, `report_cards`, `attendance`, `time_slots`, `timetable_entries`.

C'est le schéma appliqué à **chaque** école via `TenantMigrationService`, isolé du schéma `public` (Flyway n'inclut pas `public` dans le search_path des migrations tenant — la fonction `fn_update_updated_at()` doit donc y être redéfinie).

### 5.3 `db/migration/seed/V2__insert_sample_school.sql` (141 lignes)

Insère l'école de démo « Lycée Sainte Marie de Dixinn » (idempotent via `ON CONFLICT DO NOTHING`) et le catalogue tarifaire (Starter 45k/mois, Standard 120k/mois, Premium 280k/mois GNF, avec 2 mois offerts à l'engagement annuel).

---

## 6. Infrastructure transversale

- **PDF** (`infrastructure/pdf/PdfGeneratorService.java`) : génération via Thymeleaf (`TemplateEngine`) + OpenHTMLtoPDF (`PdfRendererBuilder.useFastMode()`), destinée aux bulletins de notes et reçus de paiement. **Confirmé non branché** : cette classe n'est référencée par aucun autre fichier de `src/main` — `ReportCardController.getReportCardPdf` (`/api/v1/school/report-cards/{id}/pdf`) ne l'appelle pas et retourne un placeholder texte (`"PDF not yet generated"`) au lieu d'un vrai PDF.
- **SMS** (`infrastructure/sms/`) : abstraction `SmsProvider` avec une **seule** implémentation, `LoggingSmsProvider` (journalise le SMS via SLF4J, renvoie un succès simulé avec un UUID aléatoire comme `providerMessageId` — **aucun envoi réel**). `application.yml` déclare des propriétés `app.sms.orange.*` (base-url, client-id, client-secret) mais **aucune classe Java n'implémente un provider Orange** : le SMS est donc entièrement simulé quelle que soit la configuration actuelle.
- **Storage** (`infrastructure/storage/`) : `StorageService` avec implémentation `LocalStorageService` (stockage disque local, chemin configurable `app.storage.local.base-path` — `./uploads` en dev, `/var/schoolsaas/uploads` en prod). Pas d'implémentation cloud (S3, etc.) à ce stade — limite le déploiement à une seule instance avec disque persistant, ou nécessite un volume partagé en cas de scale-out.

---

## 7. Génération de types TypeScript (lien avec le frontend)

Le `pom.xml` déclare le plugin `typescript-generator-maven-plugin`, exécuté en phase `process-classes` :
- Scanne `com.schoolsaas.**.dto.**`.
- Génère `../school-management-client/src/app/core/models/api.ts` (confirmé existant — `school-management-client` est un projet Angular présent au même niveau que ce backend, dans le même monorepo).
- Exclut les classes `*Builder`, mappe les enums en `enum` TypeScript.

Cela signifie que **le contrat d'API est piloté par le code Java** : toute modification de DTO côté backend régénère automatiquement les types côté client au build Maven — mécanisme à connaître avant de modifier une DTO manuellement côté Angular.

---

## 8. Tests et qualité

- **33 fichiers de test**, couvrant la plupart des services et contrôleurs (`*ServiceTest`, `*ControllerTest`, `*MapperTest`) pour academic, attendance, communication, dashboard, enrollment, finance, grading, identity, platform, timetable.
- `AbstractControllerTest.java` (`test/support/`) : classe de base mutualisée pour les tests de contrôleurs (33 lignes).
- Jacoco configuré dans le `pom.xml` (`prepare-agent` + rapport en phase `test`) — couverture mesurée mais pas de seuil minimal (`check`) imposé au build.
- H2 en dépendance de test, mais la base réelle est PostgreSQL (multi-tenancy schema-based non reproductible sur H2) — H2 sert probablement aux tests unitaires simples, pas aux tests d'intégration multi-tenant.

**✅ Mis à jour depuis la rédaction de ce rapport** — au moment de l'analyse initiale, la suite comptait 5 tests en échec et 5 tests skippés :
- 5 tests cassés (`AuthServiceTest`, `UserServiceTest`, `UserControllerTest`, `UserMapperTest`, `OnboardingServiceTest`) référençaient encore l'ancien enum Java `Role` et les anciens collaborateurs d'`AuthService`/`OnboardingService`, supprimés par des commits récents (`fix auth`/`fix login`/`fix connexion`) sans mise à jour des tests correspondants — corrigés (commit `95c4cea`).
- 4 tests étaient `@Disabled` (`MultiTenancyIntegrationTest`, `OpenApiGeneratorTest`, `SchoolSaasApplicationTests`, `DashboardServiceTest`), tous avec le motif générique *"skipped for Phase 6 unit test coverage"*. En pratique : `DashboardServiceTest` n'avait besoin d'aucune infrastructure (il manquait juste un `TenantContext.set(...)` avant l'appel au service) ; les trois autres ont été convertis pour tourner contre un vrai PostgreSQL éphémère via Testcontainers, à l'image de ce que faisait déjà (correctement) `MultiTenancyIntegrationTest` — corrigés (commit `b2e5456`).

Suite complète actuelle : **135 tests, 0 échec, 0 skip.**

---

## 9. Configuration par environnement

Trois fichiers : `application.yml` (commun), `application-dev.yml`, `application-prod.yml`.

| Aspect | Dev | Prod |
|---|---|---|
| Datasource | valeurs par défaut (`localhost`, `admin/admin`) | **aucune valeur par défaut** — crash au démarrage si variable absente (volontaire, évite de se connecter silencieusement à la mauvaise base) |
| Pool Hikari | 10 max / 2 idle | 20 max / 5 idle, `leak-detection-threshold: 30s` |
| SQL logging | `show-sql: true`, `format_sql: true` | désactivé |
| Flyway | `validate-on-migrate: false`, `clean-disabled: false` (permet de repartir de zéro en itérant) | `validate-on-migrate: true`, `clean-disabled: true` (interdit tout `flyway:clean` accidentel) |
| Erreurs HTTP | stacktrace visible sur `?trace=true` | **jamais** de message/stacktrace renvoyé au client (évite la fuite de noms de tables/contraintes) |
| JWT secret | valeur par défaut de dev fournie en clair dans le YAML | **obligatoire**, aucun défaut (`${JWT_SECRET}` seul dans `application.yml`) |
| CORS | localhost:4200/3000/8080 | `${FRONTEND_URL}` — **mais non lu par le code**, voir §3 |
| Swagger | actif | **désactivé** (`springdoc.api-docs.enabled: false`) |
| Actuator | tout exposé (`include: "*"`), détails toujours visibles | seul `health` exposé, détails `when-authorized`, probes liveness/readiness activées |
| Logging | `DEBUG` partout, y compris bind SQL en `TRACE` | `WARN`/`INFO`, fichier rotatif (50MB, 30 jours) |

Cette séparation dev/prod est cohérente et bien pensée dans l'ensemble ; la seule incohérence relevée est le CORS (§3).

---

## 10. Dette technique et points d'attention identifiés

1. ~~**CORS `*` en dur, config prod ignorée**~~ — **✅ corrigé** (commit `c9c3b69`), voir §3.
2. ~~**Test d'intégration multi-tenant désactivé**~~ — **✅ corrigé**, ainsi que 3 autres tests skippés pour la même raison générique (commit `b2e5456`), voir §8.
3. **TODOs fonctionnels non résolus**, tous explicitement commentés dans le code avec la phase prévue :
   - `grading/service/ReportCardService.java:187-188` — génération PDF et notification SMS du bulletin non intégrées.
   - `grading/controller/ReportCardController.java:166-185` — PDF non implémenté, nom d'élève non résolu (placeholder en dur).
   - `enrollment/service/PromotionService.java:205-219` — calcul de la classe suivante (progression curriculaire) et création de l'inscription pour l'année suivante non implémentés — **la fonctionnalité de promotion de fin d'année semble incomplète**.
   - `finance/controller/FeeStructureController.java:194-196` — un endpoint retourne littéralement `"TODO"` comme nom d'élève, calcul non implémenté (nécessite lookup `StudentEnrollment`).
   - `finance/service/PaymentService.java:86` — confirmation SMS de paiement non envoyée.
   - `timetable/controller/TimetableController.java:139-161` — construction de l'emploi du temps hebdomadaire et liste des cours par enseignant non implémentées, un champ `className` retourne aussi `"TODO"` en dur.
4. **SMS entièrement simulé** : `LoggingSmsProvider` est la seule implémentation de `SmsProvider` qui existe dans le code — aucune classe n'implémente réellement l'intégration Orange dont les propriétés (`app.sms.orange.*`) sont pourtant déclarées dans `application.yml`. Aucun SMS n'est donc envoyé en pratique, quelle que soit la configuration.
5. **Stockage local uniquement** : pas d'implémentation cloud (S3 ou équivalent) pour `StorageService`, ce qui limite le déploiement à une seule instance avec disque persistant (ou nécessite un volume partagé en cas de scale-out).
6. **Logs de diagnostic en `INFO`** dans le code multi-tenant (`SchemaMultiTenantConnectionProvider.java`, `TenantIdentifierResolver.java` — préfixe `[DIAG]`) : utile en debug mais bruyant/verbeux si laissé tel quel en production à fort trafic (chaque connexion DB loggue le tenant résolu).
7. **Header `X-Tenant-Id` mort** : autorisé en CORS mais jamais lu côté serveur — soit à retirer, soit à documenter comme réservé à un usage futur.
8. **Rôle `PARENT` non exploité côté autorisation** : la constante existe (`SystemRoleCodes.java`) mais aucun `@PreAuthorize` ne la référence — cohérent avec le fait que la communication vers les parents reste unidirectionnelle (SMS sortants) à ce stade, mais à surveiller si un futur portail parent est prévu.
9. **Contradiction de commentaire** dans `application.yml` : le profil actif par défaut est `${SPRING_PROFILES_ACTIVE:dev}`, alors que le commentaire adjacent affirme vouloir « éviter de démarrer en dev par accident sur le VPS » — le défaut réel contredit l'intention documentée ; un déploiement qui oublierait de fixer `SPRING_PROFILES_ACTIVE=prod` démarrerait silencieusement en dev.
10. **Actuator `permitAll()` non différencié par profil** : la règle Spring Security autorise `/actuator/**` sans authentification quel que soit le profil ; seule la propriété `management.endpoints.web.exposure.include` restreint réellement les endpoints exposés en prod. En dev, `include: "*"` combiné à `permitAll()` expose tous les endpoints Actuator (env, beans, threaddump...) sans authentification.
11. **Documentation existante abondante mais dispersée** : le dépôt contient déjà `DETAILED_ANALYSIS.md` (audit de conformité specs/implémentation, score global 8.2/10 "ACCEPTABLE"), `PACKAGE_STRUCTURE.md`, `SCHOOLSAAS_SPECS.md` (specs fonctionnelles F-01 à F-19), `PROMPT_JUNIE.md`, `ALL_ENTITY_CLASSES.txt` — plusieurs documents d'analyse coexistent, avec un risque de divergence entre eux au fil des évolutions du code si non maintenus en parallèle.

---

## 11. Synthèse : mauvaises pratiques, failles, qualité du code, pistes de refacto

### 11.1 Mauvaises pratiques constatées

- **Tests non maintenus avec le code** : les commits `fix auth`, `fix login`, `fix connexion` ont réécrit `AuthService`, `OnboardingService`, `User` en profondeur sans toucher aux tests correspondants — 5 tests cassés, restés rouges. Signe que ces commits ont été poussés sans lancer `mvn test` avant, ou sans revue qui l'aurait détecté.
- **Désactiver plutôt que corriger** : 4 tests étaient `@Disabled` avec le motif générique "skipped for Phase 6 unit test coverage" — alors qu'en pratique 3 d'entre eux échouaient pour une raison triviale (secret JWT manquant, mauvais chemin d'endpoint, config H2 qui ne créait aucune table) et le 4e n'avait simplement pas besoin de DB du tout. Réflexe de désactiver plutôt que diagnostiquer.
- **Logs de debug oubliés en `INFO`** : préfixe `[DIAG]` dans `TenantIdentifierResolver`, `SchemaMultiTenantConnectionProvider`, `AuthTenantService` — visiblement ajoutés pour traquer un bug de prod puis jamais retirés ni repassés en `DEBUG`/`TRACE`.
- **Code mort laissé en commentaire** au lieu d'être supprimé : `.role(request.getRole())` commenté dans `UserService`, champs commentés dans `ReportCardController`, anciennes références `Role.DIRECTOR` dans les tests (désormais corrigées). Complique la lecture et laisse deviner si c'est "à finir" ou oublié.
- **Config déclarée mais jamais branchée** : `app.cors.allowed-origins` existait dans les YAML dev/prod depuis le début mais `SecurityConfig` l'ignorait totalement (voir §3 — corrigé depuis).
- **`"TODO"` en dur retourné par de vrais endpoints** (`FeeStructureController.studentName("TODO")`, `TimetableController.className("TODO")`) — un TODO en commentaire est normal, un TODO qui part en réponse HTTP ne devrait pas passer en revue.
- **Commits directs sur `develop`** avec messages très courts (`fix`, `fix auth`, `fix login`) — pas de trace du "pourquoi" dans l'historique, toute la charge de compréhension repose sur la lecture du diff.
- **Documentation multipliée sans source de vérité unique** : `DETAILED_ANALYSIS.md`, `PACKAGE_STRUCTURE.md`, `SCHOOLSAAS_SPECS.md`, `PROMPT_JUNIE.md`, `ALL_ENTITY_CLASSES.txt` (+ ce rapport) — risque réel de divergence si un seul de ces fichiers est mis à jour après un changement de code et pas les autres.

### 11.2 Failles constatées

- **CORS `*` en dur** — voir §3, corrigé.
- **Actuator `permitAll()` sans distinction de profil** dans `SecurityConfig` — la seule protection en prod vient de `management.endpoints.web.exposure.include: health`, pas d'une règle Spring Security différenciée. Pas de défense en profondeur : si cette propriété est un jour élargie par erreur, plus rien ne bloque l'accès aux endpoints sensibles (`env`, `beans`, `threaddump`...).
- **Secret JWT de dev committé en clair** dans `application-dev.yml` — risque faible (dev only) mais mauvaise habitude qui peut se reproduire ailleurs.
- **Aucune protection anti brute-force visible** sur `/api/v1/auth/login` (pas de rate-limiting, pas de verrouillage de compte après N échecs) — non confirmée absente ailleurs (proxy, WAF), mais rien dans le code applicatif ne s'en charge.
- **SMS entièrement simulé** (`LoggingSmsProvider` seul, pas d'implémentation Orange) — pas une faille de sécurité en soi, mais un risque fonctionnel si l'on suppose à tort que les notifications partent réellement.

### 11.3 Qualité du code : architecture solide, exécution inégale

L'**architecture** est de bonne facture : découpage par domaine cohérent, choix multi-tenant schema-per-tenant bien argumenté, et fait rare — les migrations SQL expliquent systématiquement le *pourquoi* des décisions (contraintes, corrections de bugs passés), ce qui aide beaucoup à la maintenance. La séparation `platform`/`identity` avec passage par UUID plutôt que par relation JPA inter-schema est un choix mûri, pas accidentel.

Le point faible n'est pas la conception mais la **discipline d'exécution** : tests qui dérivent du code, TODOs qui s'accumulent sans ticket associé, config qui diverge silencieusement du code. Profil d'un projet développé vite (probablement avec beaucoup d'itérations assistées par IA, vu le volume de docs générées) sans garde-fou de CI qui bloquerait un merge avec des tests rouges ou skippés.

### 11.4 Pistes de refacto

1. **Mutualiser la validation du nom de schéma** — le même regex (`^[a-zA-Z0-9_]+$` / `^[a-z0-9_]+$`) et la même logique de validation apparaissent séparément dans `SchemaMultiTenantConnectionProvider`, `OnboardingService`, `DashboardService`. Un seul utilitaire partagé réduirait le risque d'oublier la validation dans un futur endroit qui concatène du SQL.
2. **Supprimer le code mort** plutôt que le commenter (champs `role` fantômes, DTO fields commentés dans `ReportCardController`).
3. **Consolider la documentation** — un seul document vivant (ou archiver les anciens) plutôt que 5+ fichiers qui peuvent diverger.
4. **Étendre le pattern `@ConfigurationProperties`** introduit pour `CorsProperties` aux autres blocs `app.*` (sms, storage, scheduler) actuellement lus via `@Value` épars — plus cohérent, plus facile à tester.
5. **Tracker les TODOs comme des issues** plutôt que des commentaires "Phase 4/5" sans référence externe — sinon ils se perdent, comme les 4 tests skippés qui l'ont été.
6. **Ajouter un garde-fou CI** qui échoue le build si un test est skip/disabled sans justification validée en revue — pour éviter que le pattern "on désactive plutôt que corriger" se reproduise.

---

## 12. Résumé exécutif

Le backend est structuré avec soin : séparation modulaire par domaine cohérente, choix multi-tenant (schema-per-tenant) bien argumenté et documenté directement dans le SQL (les commentaires expliquent systématiquement le *pourquoi*, pas seulement le *quoi*, avec traçabilité de bugs corrigés). La configuration dev/prod distingue correctement les postures de sécurité.

Corrigé depuis la version initiale de ce rapport : le CORS (§3), les 5 tests cassés et les 4 tests skippés (§8, §11.1). Restent à traiter avant une mise en production sérieuse : compléter les fonctionnalités marquées TODO (bulletins PDF, notifications SMS, promotion d'élèves, emploi du temps agrégé), et adresser les points de discipline listés en §11 (logs de debug, code mort, documentation dispersée, absence de garde-fou CI sur les tests).
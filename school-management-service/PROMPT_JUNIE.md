# Prompt Junie — SchoolSaaS Backend

## TON RÔLE

Tu es un développeur backend senior Java/Spring Boot. Ta mission est de générer
le code complet et compilable du backend de SchoolSaaS, une application SaaS de
gestion des écoles privées ciblant la Guinée et l'Afrique de l'Ouest.

Tu dois produire un code de qualité production : propre, cohérent, sans raccourcis,
sans TODO vides, sans méthodes non implémentées. Chaque classe générée doit être
immédiatement utilisable sans modification manuelle.

---

## DOCUMENTS DE RÉFÉRENCE (joints à ce prompt)

Tu as accès à 3 documents que tu DOIS lire en entier avant d'écrire la moindre
ligne de code :

1. **SCHOOLSAAS_SPECS.md** — Les 19 fonctionnalités détaillées avec endpoints,
   règles métier, format JSON, validations et comportements attendus.
   C'est ta source de vérité fonctionnelle.

2. **PACKAGE_STRUCTURE.md** — L'arborescence exacte des packages Java et les
   règles d'architecture à respecter absolument (dépendances inter-domaines,
   nommage, DTO vs entité).

3. **schoolsaas_diagram_v2.png** — Le diagramme de classes UML avec les 25
   entités, leurs attributs, leurs types et leurs relations.
   Utilise-le pour générer les entités JPA et les migrations Flyway.

---

## STACK TECHNIQUE

- **Java 21** avec records, sealed classes et pattern matching si pertinent
- **Spring Boot 3.3.4** (spring-boot-starter-parent)
- **PostgreSQL 16** avec multitenancy schema-per-tenant
- **Flyway** pour les migrations (public schema + tenant schema)
- **Hibernate 6** avec `MultiTenantConnectionProvider` et `CurrentTenantIdentifierResolver`
- **Spring Security 6** stateless JWT
- **jjwt 0.12.6** pour les tokens JWT
- **MapStruct 1.6.2** pour les conversions entité ↔ DTO (ne pas faire les mappings à la main)
- **Lombok** pour réduire le boilerplate (@Getter, @Builder, @RequiredArgsConstructor, etc.)
- **Flyway** migrations dans `src/main/resources/db/migration/public/` et `tenant/`
- **SpringDoc OpenAPI 2.6.0** pour la documentation Swagger
- **Caffeine** pour le cache
- **Thymeleaf + OpenHTMLtoPDF** pour la génération PDF
- **Micrometer** pour les métriques (déjà inclus via Actuator)

Le `pom.xml` complet est fourni dans SCHOOLSAAS_SPECS.md.

---

## ARCHITECTURE — RÈGLES NON NÉGOCIABLES

### 1. Package par domaine (Approche 2)

```
com.schoolsaas/
├── config/          (multitenancy, security, cache, openapi)
├── common/          (BaseEntity, ApiResponse, BusinessException, utils)
├── platform/        (School, Subscription — schema PUBLIC)
├── identity/        (User, Teacher, Auth)
├── academic/        (AcademicYear, Term, Level, SchoolClass, Subject, ClassSubject)
├── enrollment/      (Student, StudentEnrollment, Promotion)
├── timetable/       (TimeSlot, TimetableEntry)
├── grading/         (Grade, ReportCard, Ranking)
├── attendance/      (Attendance)
├── finance/         (FeeStructure, StudentFee, Payment, PaymentAllocation)
├── communication/   (SmsTemplate, SmsLog, SmsScheduler)
├── dashboard/       (stats cross-domain, pas d'entités propres)
└── infrastructure/  (sms providers, storage, pdf)
```

### 2. Règles inter-domaines

- Un domaine appelle les **services** d'un autre domaine, jamais ses entités JPA.
- Les domaines communiquent via des **UUID/IDs**, jamais via des objets entiers.
- Les entités JPA ne sortent **jamais** des couches service/repository.
  Les controllers reçoivent des RequestDTO et retournent des ResponseDTO via MapStruct.
- `grading/` peut appeler `enrollmentService` mais ne peut pas faire
  `import com.schoolsaas.enrollment.entity.Student`.

### 3. Nommage

| Composant    | Convention              | Exemple                        |
|--------------|-------------------------|--------------------------------|
| Entité JPA   | NomMétier               | `Student`, `Payment`           |
| Repository   | NomMétier + Repository  | `StudentRepository`            |
| Service      | NomMétier + Service     | `PaymentService`               |
| Controller   | NomMétier + Controller  | `PaymentController`            |
| Request DTO  | Verbe + Nom + Request   | `CreatePaymentRequest`         |
| Response DTO | Nom + [Detail] Response | `PaymentResponse`              |
| Mapper       | Nom + Mapper            | `PaymentMapper` (MapStruct)    |

### 4. Note importante sur le mot réservé Java

L'entité classe s'appelle **`SchoolClass`** (pas `Class` qui est réservé en Java).
Table en base : `classes`. Annotation : `@Table(name = "classes")`.

---

## MULTITENANCY — FONCTIONNEMENT ATTENDU

Le système utilise un schema PostgreSQL par école (tenant). Voici le flux exact :

1. À chaque requête HTTP, le `JwtAuthenticationFilter` extrait le claim
   `tenantId` (= schemaName) du JWT et appelle `TenantContext.set(schemaName)`.

2. Hibernate appelle `TenantIdentifierResolver.resolveCurrentTenantIdentifier()`
   qui retourne `TenantContext.get()` (ou "public" si null).

3. `SchemaMultiTenantConnectionProvider.getConnection(tenant)` exécute
   `SET search_path = {schemaName}, public` sur chaque connexion JDBC.

4. Dans le `finally` du filtre, `TenantContext.clear()` nettoie le ThreadLocal
   et `MDC.remove("tenantId")` nettoie les logs.

**Important :** les entités du package `platform/` ont
`@Table(schema = "public")` car elles vivent dans le schema public.
Toutes les autres entités n'ont pas d'attribut `schema` dans `@Table`.

**Validation du schemaName** : avant tout `SET search_path`, vérifier que
le nom matche `^[a-zA-Z0-9_]+$` — lever `IllegalArgumentException` sinon.

---

## BASE DE DONNÉES

### Migrations Flyway

```
src/main/resources/db/migration/
├── public/
│   └── V1__init_public_schema2.sql    ← tables public: schools, subscription_plans,
│                                        school_subscriptions, subscription_payments
└── tenant/
    └── V1__init_tenant_schema2.sql    ← toutes les tables métier + triggers + vue matérialisée
```

La migration `tenant/V1` est appliquée par `TenantMigrationService` lors
de l'onboarding de chaque nouvelle école (pas au démarrage de l'app).

### Triggers PostgreSQL obligatoires dans V1__init_tenant_schema2.sql

```sql
-- 1. Matricule élève auto-généré : EL-YYYY-NNNN
CREATE OR REPLACE FUNCTION fn_generate_student_number() RETURNS TRIGGER ...

-- 2. Numéro de reçu auto-généré : REC-YYYYMM-NNNN
CREATE OR REPLACE FUNCTION fn_generate_receipt_number() RETURNS TRIGGER ...

-- 3. Recalcul statut StudentFee après PaymentAllocation
--    Si remaining <= 0 → PAID | Si paid > 0 → PARTIAL | Si dueDate < today → OVERDUE
CREATE OR REPLACE FUNCTION fn_recalculate_fee_status() RETURNS TRIGGER ...

-- 4. updated_at automatique sur toutes les tables concernées
CREATE OR REPLACE FUNCTION fn_update_updated_at() RETURNS TRIGGER ...
```

### Vue matérialisée dashboard

```sql
CREATE MATERIALIZED VIEW mv_dashboard_stats AS
SELECT
    COUNT(DISTINCT e.id) FILTER (WHERE e.status = 'enrolled') AS active_students,
    COUNT(DISTINCT e.student_id)                               AS total_students,
    COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender='M')  AS male_students,
    COUNT(DISTINCT e.student_id) FILTER (WHERE s.gender='F')  AS female_students,
    COALESCE(SUM(sf.amount_due - sf.discount_amount), 0)       AS total_fees_expected,
    COALESCE(SUM(sf.amount_paid), 0)                           AS total_fees_collected,
    CASE WHEN SUM(sf.amount_due)>0
         THEN ROUND(100.0 * SUM(sf.amount_paid)/SUM(sf.amount_due-sf.discount_amount),1)
         ELSE 0 END                                            AS collection_rate_pct,
    COUNT(DISTINCT e.id) FILTER (WHERE sf.status IN ('unpaid','partial','overdue')) AS students_with_debt,
    COUNT(DISTINCT e.id) FILTER (WHERE sf.status = 'overdue') AS students_overdue,
    COUNT(sl.id) FILTER (WHERE DATE(sl.created_at) = CURRENT_DATE) AS sms_today,
    COUNT(sl.id) FILTER (WHERE sl.created_at >= DATE_TRUNC('month',NOW())) AS sms_this_month
FROM enrollments e
JOIN students s ON e.student_id = s.id
LEFT JOIN student_fees sf ON sf.enrollment_id = e.id
CROSS JOIN (SELECT COUNT(*) AS pending_grade_entries FROM grades WHERE value IS NULL) g
LEFT JOIN sms_logs sl ON TRUE
WHERE e.academic_year_id = (SELECT id FROM academic_years WHERE is_current=TRUE LIMIT 1);

CREATE UNIQUE INDEX ON mv_dashboard_stats((1));
```

---

## SÉCURITÉ

### JWT Claims

```json
{
  "sub": "user-uuid",
  "tenantId": "school_ecole_lumiere_conakry",
  "tenantUuid": "school-uuid",
  "roles": ["DIRECTOR"],
  "email": "directeur@ecole.gn",
  "type": "access",
  "iss": "schoolsaas-api",
  "jti": "unique-token-id",
  "iat": 1700000000,
  "exp": 1700003600
}
```

### Matrice des droits

| Endpoint                            | DIRECTOR | TEACHER | ACCOUNTANT |
|-------------------------------------|:--------:|:-------:|:----------:|
| POST /auth/**                       | ✅ public | ✅ public | ✅ public |
| GET /school/users                   | ✅       | ❌       | ❌        |
| POST/PUT/DELETE /school/users       | ✅       | ❌       | ❌        |
| GET /school/students                | ✅       | ✅       | ✅        |
| POST/PUT /school/students           | ✅       | ❌       | ✅        |
| DELETE /school/students             | ✅       | ❌       | ❌        |
| /school/academic-years, /classes    | ✅ (W)   | ✅ (R)  | ✅ (R)   |
| GET /school/grades                  | ✅       | ✅       | ❌        |
| POST/PUT /school/grades             | ✅       | ✅*      | ❌        |
| /school/report-cards                | ✅       | ✅ (R)  | ❌        |
| POST publish /report-cards          | ✅       | ❌       | ❌        |
| /school/attendance                  | ✅       | ✅       | ❌        |
| /school/fee-structures              | ✅ (W)   | ❌       | ✅ (R)   |
| /school/payments                    | ✅       | ❌       | ✅        |
| DELETE /school/payments             | ✅       | ❌       | ❌        |
| /school/sms/**                      | ✅       | ❌       | ✅ (R)   |
| /school/dashboard                   | ✅       | ✅       | ✅        |
| /platform/** (super-admin)          | ❌       | ❌       | ❌        |

*TEACHER : uniquement pour les ClassSubject où il est affecté (teacherId = currentUser.id)

### Implémentation Spring Security

```java
// Dans chaque méthode service concernée par la restriction TEACHER :
UUID currentUserId = UUID.fromString(authenticatedUser.getUserId());
ClassSubject cs = classSubjectRepository.findById(classSubjectId).orElseThrow(...);
if (hasRole("TEACHER") && !cs.getTeacherId().equals(currentUserId)) {
    throw BusinessException.forbidden("UNAUTHORIZED_SUBJECT",
        "Vous n'êtes pas affecté à cette matière");
}
```

---

## CONVENTIONS API

### Format de réponse systématique

```java
// Succès simple
ApiResponse.ok(data)
ApiResponse.ok(data, "Message de succès")

// Succès paginé
ApiResponse.paged(pageData, PageMeta.of(page))

// Erreur
ApiResponse.error("CODE_ERREUR", "Message lisible")
```

### Codes d'erreur métier standardisés

| Code | HTTP | Situation |
|------|------|-----------|
| `VALIDATION_ERROR` | 400 | Bean validation échoue |
| `ALLOCATION_MISMATCH` | 400 | Somme allocations ≠ montant paiement |
| `FEE_ALREADY_PAID` | 400 | Tentative de payer un frais déjà PAID |
| `OVER_PAYMENT` | 400 | Montant alloué > restant dû |
| `GRADES_ENTRY_CLOSED` | 400 | Saisie notes fermée sur ce trimestre |
| `DUPLICATE_GRADE` | 409 | Note déjà saisie pour ce libellé |
| `ATTENDANCE_ALREADY_RECORDED` | 409 | Présence déjà enregistrée |
| `INVALID_CREDENTIALS` | 401 | Login incorrect |
| `UNAUTHORIZED_SUBJECT` | 403 | Enseignant non affecté à cette matière |
| `NOT_FOUND` | 404 | Ressource introuvable |
| `TENANT_NOT_FOUND` | 404 | École inactive ou inexistante |
| `SLUG_ALREADY_TAKEN` | 409 | Slug déjà utilisé à l'onboarding |
| `CANCELLATION_WINDOW_EXPIRED` | 400 | Annulation paiement > 24h |
| `CLASS_TIMESLOT_CONFLICT` | 409 | Classe déjà occupée sur ce créneau |
| `TEACHER_TIMESLOT_CONFLICT` | 409 | Enseignant déjà occupé sur ce créneau |

### Pagination

Tous les endpoints de liste acceptent :
```
?page=0&size=20&sortBy=lastName&sortDir=ASC
```
Taille maximale : 100. Défaut : 20.

---

## MODULE SMS — DÉTAILS D'IMPLÉMENTATION

### Interface SmsProvider

```java
public interface SmsProvider {
    SmsResult send(String to, String message);
    List<SmsResult> sendBulk(List<String> recipients, String message);
    String getProviderName();
}
```

### SmsResult

```java
@Builder @Getter
public class SmsResult {
    boolean success;
    String providerMessageId;
    String errorCode;
    String errorMessage;
    String recipientPhone;
}
```

### Sélection du provider (SmsProviderConfig)

```java
@Bean @Primary
public SmsProvider activeSmsProvider(...) {
    return switch (providerName) {
        case "orange"  -> orangeSmsProvider;
        case "twilio"  -> twilioSmsProvider;
        default        -> loggingSmsProvider; // dev
    };
}
```

### Templates pré-chargés à l'onboarding (dans V1__init_tenant_schema2.sql)

```sql
INSERT INTO sms_templates (code, category, content_fr, variables) VALUES
('fee_reminder', 'FINANCIAL',
 'Bonjour {{parent_name}}, les frais de {{student_name}} ({{class_name}}) s''élèvent à {{amount_due}} GNF. Merci de régler avant le {{due_date}}.',
 '["parent_name","student_name","class_name","amount_due","due_date"]'),

('payment_received', 'FINANCIAL',
 'Bonjour {{parent_name}}, paiement de {{amount_paid}} GNF reçu pour {{student_name}}. Reste dû: {{remaining}} GNF. Reçu n°{{receipt_number}}.',
 '["parent_name","student_name","amount_paid","remaining","receipt_number"]'),

('report_card_published', 'ACADEMIC',
 'Bonjour {{parent_name}}, le bulletin de {{term_name}} de {{student_name}} est disponible. Moy: {{average}}/20. Rang: {{rank}}/{{class_size}}.',
 '["parent_name","term_name","student_name","average","rank","class_size"]'),

('absence_notification', 'ACADEMIC',
 'Bonjour {{parent_name}}, votre enfant {{student_name}} était absent(e) le {{date}}. Merci de nous contacter.',
 '["parent_name","student_name","date"]');
```

### SmsTemplateEngine

Résoudre les variables `{{key}}` avec regex `\{\{([^}]+)\}\}`.
Logger un WARNING si une variable est présente dans le template mais absente de la Map.
Vérifier que le message résolu ne dépasse pas 160 caractères (logguer WARNING si dépassement).

---

## SCHEDULERS

```java
@Scheduled(cron = "0 0 8 * * MON-FRI")  // Rappels frais impayés
public void sendFeeReminders() {
    // Pour chaque school active :
    //   TenantContext.set(school.schemaName)
    //   smsService.sendBulkFeeReminders()
    //   TenantContext.clear()
}

@Scheduled(cron = "0 0 0 * * *")         // Marquer frais en retard
public void markOverdueFees() {
    // UPDATE student_fees SET status='overdue'
    // WHERE status IN ('unpaid','partial') AND due_date < CURRENT_DATE
}

@Scheduled(cron = "0 */15 * * * *")      // Rafraîchir la vue matérialisée
public void refreshDashboard() {
    // REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_stats
}
```

---

## GÉNÉRATION DE CODE — ORDRE À RESPECTER

Génère les fichiers dans cet ordre précis pour éviter les dépendances manquantes :

```
ÉTAPE 1 — Infrastructure de base
  pom.xml
  application.yml (profils dev et prod)
  src/main/resources/db/migration/public/V1__init_public_schema2.sql
  src/main/resources/db/migration/tenant/V1__init_tenant_schema2.sql

ÉTAPE 2 — Config et Common
  config/multitenancy/ (TenantContext, TenantIdentifierResolver, SchemaMultiTenantConnectionProvider)
  config/security/ (SecurityConfig, JwtService, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, AuthenticatedUser)
  config/CacheConfig.java
  config/AsyncConfig.java
  common/entity/BaseEntity.java
  common/dto/ApiResponse.java
  common/exception/ (BusinessException, GlobalExceptionHandler)
  common/util/ (PhoneUtils, SlugUtils)

ÉTAPE 3 — Platform (schema public)
  platform/entity/ → platform/repository/ → platform/service/ → platform/controller/ → platform/dto/
  platform/scheduler/SubscriptionScheduler.java

ÉTAPE 4 — Identity
  identity/entity/ → identity/repository/ → identity/service/ → identity/controller/ → identity/dto/

ÉTAPE 5 — Academic
  academic/entity/ → academic/repository/ → academic/service/ → academic/controller/ → academic/dto/

ÉTAPE 6 — Enrollment
  enrollment/entity/ → enrollment/repository/ → enrollment/service/ → enrollment/controller/ → enrollment/dto/

ÉTAPE 7 — Finance
  finance/entity/ → finance/repository/ → finance/service/ → finance/controller/ → finance/dto/

ÉTAPE 8 — Communication
  infrastructure/sms/ (SmsProvider, SmsResult, LoggingSmsProvider, OrangeSmsProvider, SmsProviderConfig)
  communication/entity/ → communication/repository/ → communication/service/ → communication/scheduler/ → communication/controller/ → communication/dto/

ÉTAPE 9 — Grading, Attendance, Timetable
  grading/ → attendance/ → timetable/

ÉTAPE 10 — Dashboard
  infrastructure/storage/ (StorageService, LocalStorageService)
  infrastructure/pdf/ (PdfGeneratorService)
  dashboard/service/ → dashboard/controller/ → dashboard/dto/

ÉTAPE 11 — Tests
  Tests unitaires pour PaymentService, SmsService, AuthService
  Test d'intégration avec Testcontainers PostgreSQL
  SchoolSaasApplicationTests (contextLoads)
```

---

## POINTS D'ATTENTION CRITIQUES

1. **`SchoolClass` pas `Class`** — le nom `Class` est réservé en Java.
   Annoter avec `@Table(name = "classes")`.

2. **Entités platform avec schema explicite** — `@Table(name="schools", schema="public")`.
   Les entités métier n'ont PAS d'attribut `schema` dans `@Table`.

3. **isCurrent unique** — Pour AcademicYear et Term, l'unicité de `isCurrent=true`
   doit être gérée au niveau service (UPDATE SET is_current=false WHERE is_current=true
   AVANT de setter is_current=true sur la nouvelle).

4. **Génération automatique des StudentFee** — À chaque nouvelle inscription
   (`EnrollmentService.enroll()`), appeler immédiatement
   `studentFeeService.generateFeesForEnrollment(enrollment)`.

5. **SMS non bloquant** — Les envois SMS doivent être dans un bloc `try/catch`
   qui ne fait que logger l'erreur. Un SMS qui échoue ne doit jamais faire échouer
   un paiement ou une inscription.

6. **Validation numéros guinéens** — Format attendu : `+224XXXXXXXXX` (13 chars).
   Normaliser automatiquement `00224...` et `224...` vers `+224...`.

7. **Annulation paiement = soft delete** — Ne jamais `DELETE` un paiement.
   Ajouter `[ANNULÉ par {userId} le {date}] {reason}` dans le champ `notes`.

8. **Génération PDF asynchrone** — `ReportCardService.publish()` génère le PDF
   en `@Async` et met à jour `pdfUrl` quand c'est prêt. Retourner 202 Accepted.

9. **Cache dashboard** — `@Cacheable(value="dashboard_stats", key="'global'")`.
   `@CacheEvict` déclenché par le scheduler toutes les 15 minutes.

10. **MapStruct + Lombok** — Configurer les deux dans `maven-compiler-plugin`
    `annotationProcessorPaths` (Lombok DOIT être avant MapStruct dans la liste).

---

## LIVRABLES ATTENDUS

À la fin de la génération, le projet doit :

- [ ] Compiler sans erreur avec `mvn clean package -DskipTests`
- [ ] Démarrer avec `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- [ ] Exposer `/actuator/health` → `{"status":"UP"}`
- [ ] Exposer `/swagger-ui/index.html` avec tous les endpoints documentés
- [ ] Passer les migrations Flyway au démarrage (schema public)
- [ ] Les tests passent avec `mvn test` (Testcontainers PostgreSQL)

Structure de livraison :
```
schoolsaas-backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/schoolsaas/   ← tout le code Java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── templates/         ← templates Thymeleaf pour PDF
│   │       └── db/migration/
│   │           ├── public/
│   │           └── tenant/
│   └── test/
│       └── java/com/schoolsaas/
└── .env.example
```

---

## INSTRUCTION FINALE

Commence par lire les 3 documents joints dans leur intégralité.
Ensuite génère le code dans l'ordre des étapes 1 à 11.
Pour chaque étape, crée tous les fichiers de l'étape avant de passer à la suivante.
Ne génère pas de placeholder, de `// TODO` vide ou de méthode qui lance
`UnsupportedOperationException`. Chaque méthode doit être implémentée.
Si tu as un doute sur une règle métier, réfère-toi à SCHOOLSAAS_SPECS.md.

 # SchoolSaaS — Spécifications complètes pour génération de code

## CONTEXTE TECHNIQUE

**Stack :** Java 21 · Spring Boot 3.3.4 · PostgreSQL 16 · Flyway · Hibernate Multitenancy
**Architecture packages :** Approche 2 — Package par domaine (`com.schoolsaas`)
**Multitenancy :** Schema-per-tenant PostgreSQL (SET search_path via ThreadLocal)
**Auth :** JWT (jjwt 0.12.6) · BCrypt(12) · access token 1h · refresh token 7j
**Autres libs :** MapStruct · Lombok · SpringDoc OpenAPI · Caffeine · Thymeleaf · OpenHTMLtoPDF · Micrometer

**Package root :** `com.schoolsaas`
**Packages :** config/multitenancy · config/security · common · platform · identity · academic · enrollment · timetable · grading · attendance · finance · communication · dashboard · infrastructure

**Conventions API :**
- Base : `/api/v1/`
- Plateforme (super-admin) : `/api/v1/platform/**`
- École (utilisateurs connectés) : `/api/v1/school/**`
- Public : `/api/v1/auth/**` et `/api/v1/platform/onboard`
- Réponse systématique : `ApiResponse<T> { success, data, error, message, pagination }`
- Pagination : paramètres `page` (défaut 0), `size` (défaut 20, max 100), `sortBy`, `sortDir`
- Erreurs métier : `BusinessException(code, message, HttpStatus)` → capturée par `GlobalExceptionHandler`

---

## MODÈLE DE DONNÉES (se référer au diagramme de classes joint)

25 entités réparties dans les domaines :
- **platform/** : School, SubscriptionPlan, SchoolSubscription, SubscriptionPayment
- **identity/** : User, Teacher
- **academic/** : AcademicYear, Term, Level, SchoolClass (≠ java.lang.Class), Subject, ClassSubject
- **enrollment/** : Student, StudentEnrollment
- **timetable/** : TimeSlot, TimetableEntry
- **grading/** : Grade, ReportCard
- **attendance/** : Attendance
- **finance/** : FeeStructure, StudentFee, Payment, PaymentAllocation
- **communication/** : SmsTemplate, SmsLog

**Enums :** Role (DIRECTOR·TEACHER·ACCOUNTANT·PARENT) · YearStatus (ACTIVE·CLOSED) · StudentStatus (ACTIVE·LEFT·GRADUATED) · EnrollmentStatus (ENROLLED·TRANSFERRED·WITHDRAWN·GRADUATED) · FeeStatus (UNPAID·PARTIAL·PAID·OVERDUE·WAIVED) · EvaluationType (DEVOIR·COMPOSITION·ORAL·TP) · PaymentMethod (CASH·ORANGE_MONEY·MTN_MONEY·WAVE·BANK_TRANSFER·CHECK) · DayOfWeek (MONDAY..SATURDAY)

---

# PHASE 1 — MVP (premières écoles payantes)

---

## F-01 · Onboarding école (platform/)

**Description :** Inscription d'une nouvelle école = création d'un tenant (schema PostgreSQL) + migration Flyway + premier compte DIRECTOR.

**Endpoint public :**
```
POST /api/v1/platform/onboard
```

**Request body :**
```json
{
  "schoolName": "string (obligatoire)",
  "slug": "string (obligatoire, unique, lowercase, tirets, ex: ecole-lumiere-conakry)",
  "email": "string (email valide, unique)",
  "phone": "string (optionnel)",
  "city": "string",
  "countryCode": "GN (défaut)",
  "planCode": "starter|standard|premium",
  "directorFirstName": "string (obligatoire)",
  "directorLastName": "string (obligatoire)",
  "directorPassword": "string (min 8 chars)",
  "timezone": "Africa/Conakry (défaut)",
  "currency": "GNF (défaut)"
}
```

**Logique (OnboardingService) :**
1. Vérifier unicité du slug et de l'email → erreur 409 CONFLICT si déjà pris
2. Créer l'entité `School` dans le schema `public` avec `status=trial`
3. Calculer `schemaName` = `school_` + slug avec tirets remplacés par underscores
4. Créer le schema PostgreSQL : `CREATE SCHEMA IF NOT EXISTS {schemaName}`
5. Appliquer les migrations Flyway du dossier `classpath:db/migration/tenant` sur ce nouveau schema
6. Dans le schema du tenant, créer le premier `User` avec `role=DIRECTOR`, mot de passe hashé BCrypt
7. Créer la `SchoolSubscription` avec le plan choisi, `startDate=today`, `endDate=today+30j` (trial)
8. Retourner le slug, le schemaName et un message de bienvenue

**Réponse 201 :**
```json
{
  "tenantSlug": "ecole-lumiere-conakry",
  "schemaName": "school_ecole_lumiere_conakry",
  "schoolName": "École Lumière Conakry",
  "message": "École créée. Connectez-vous avec votre email directeur."
}
```

**Erreurs :** 409 si slug ou email déjà utilisé · 400 si planCode invalide

---

## F-02 · Authentification (identity/)

### F-02a · Login

```
POST /api/v1/auth/login    [PUBLIC]
```

**Request :**
```json
{
  "tenantSlug": "ecole-lumiere-conakry",
  "email": "directeur@ecole.gn",
  "password": "motdepasse123"
}
```

**Logique (AuthService) :**
1. Charger le `School` via `slug=tenantSlug AND status IN (active, trial)` → 404 si introuvable
2. Setter `TenantContext.set(school.schemaName)`
3. Charger le `User` via `email AND isActive=true` → 401 si introuvable
4. Vérifier le mot de passe avec BCrypt → 401 si incorrect
5. Mettre à jour `user.lastLoginAt = now()`
6. Générer `accessToken` (1h) et `refreshToken` (7j) avec claims : `userId, tenantSchemaName, tenantId, roles, email`

**Réponse 200 :**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "fullName": "Mamadou Bah",
    "email": "directeur@ecole.gn",
    "roles": ["DIRECTOR"],
    "tenantId": "uuid",
    "schoolName": "École Lumière Conakry"
  }
}
```

**Erreurs :** 401 INVALID_CREDENTIALS (message unique "Identifiants incorrects" — ne pas indiquer lequel est faux)

### F-02b · Refresh token

```
POST /api/v1/auth/refresh    [PUBLIC]
Body: { "refreshToken": "eyJ..." }
```
Valide le refresh token → génère un nouvel accessToken + nouveau refreshToken. 401 si expiré ou invalide.

### F-02c · Logout

```
POST /api/v1/auth/logout    [AUTHENTIFIÉ]
Body: { "refreshToken": "eyJ..." }
```
Phase 1 : côté client uniquement (supprimer les tokens localement). Logger l'action. Retourner 200.

---

## F-03 · Gestion des utilisateurs (identity/)

**Accès :** Seul le DIRECTOR peut créer/modifier/désactiver des utilisateurs.

```
GET    /api/v1/school/users               [DIRECTOR]
GET    /api/v1/school/users/{id}          [DIRECTOR]
POST   /api/v1/school/users               [DIRECTOR]
PUT    /api/v1/school/users/{id}          [DIRECTOR]
DELETE /api/v1/school/users/{id}          [DIRECTOR]  → soft delete (isActive=false)
```

**Champs User :** firstName, lastName, email (unique dans le tenant), phone, role (DIRECTOR·TEACHER·ACCOUNTANT), avatarUrl, passwordHash, isActive, lastLoginAt

**Validation création :**
- email obligatoire et unique dans le tenant
- password min 8 caractères, hashé BCrypt avant persistance
- role doit être DIRECTOR, TEACHER ou ACCOUNTANT

**Règle :** Un utilisateur avec role=TEACHER doit avoir une entrée correspondante dans la table `teachers` créée automatiquement à la création du user.

**Champs Teacher supplémentaires :** employeeNumber (auto-généré ou saisi), hireDate, specialty, qualification

**Réponse liste :** `Page<UserResponse>` avec pagination

---

## F-04 · Années scolaires (academic/)

```
GET    /api/v1/school/academic-years              [ALL_ROLES]
GET    /api/v1/school/academic-years/{id}         [ALL_ROLES]
GET    /api/v1/school/academic-years/current      [ALL_ROLES]
POST   /api/v1/school/academic-years              [DIRECTOR]
PUT    /api/v1/school/academic-years/{id}         [DIRECTOR]
POST   /api/v1/school/academic-years/{id}/close   [DIRECTOR]
```

**Champs :** label (ex: "2024-2025", unique), startDate, endDate, isCurrent (boolean), status (ACTIVE·CLOSED)

**Règles métier :**
- `isCurrent=true` est unique : setter isCurrent sur une année remet toutes les autres à false (UPDATE SQL)
- Une année CLOSED ne peut plus être modifiée
- La clôture d'une année déclenche la validation des promotions en attente (appel PromotionService — Phase 2)
- Une nouvelle année peut être créée même si une autre est ACTIVE (pour préparer l'avenir)

---

## F-05 · Trimestres (academic/)

```
GET    /api/v1/school/academic-years/{yearId}/terms        [ALL_ROLES]
GET    /api/v1/school/terms/{id}                          [ALL_ROLES]
POST   /api/v1/school/academic-years/{yearId}/terms        [DIRECTOR]
PUT    /api/v1/school/terms/{id}                          [DIRECTOR]
POST   /api/v1/school/terms/{id}/open-grades-entry        [DIRECTOR]
POST   /api/v1/school/terms/{id}/close-grades-entry       [DIRECTOR]
```

**Champs :** name ("1er Trimestre"), termNumber (1·2·3), startDate, endDate, isCurrent, gradesEntryOpen

**Règles :**
- termNumber unique par année scolaire
- `isCurrent=true` unique parmi les trimestres d'une année
- `gradesEntryOpen` : seul le DIRECTOR peut ouvrir/fermer la saisie des notes
- Les enseignants ne peuvent saisir des notes QUE si `gradesEntryOpen=true` sur le trimestre courant

---

## F-06 · Niveaux et classes (academic/)

### Niveaux

```
GET    /api/v1/school/levels          [ALL_ROLES]
POST   /api/v1/school/levels          [DIRECTOR]
PUT    /api/v1/school/levels/{id}     [DIRECTOR]
```

**Champs Level :** name (unique, ex: "Primaire"), orderIndex (pour le tri)

**Pré-rempli à l'onboarding :** Primaire (1), Collège (2), Lycée (3)

### Classes (SchoolClass)

```
GET    /api/v1/school/classes                                    [ALL_ROLES]
GET    /api/v1/school/classes/{id}                               [ALL_ROLES]
GET    /api/v1/school/academic-years/{yearId}/classes            [ALL_ROLES]
POST   /api/v1/school/classes                                    [DIRECTOR]
PUT    /api/v1/school/classes/{id}                               [DIRECTOR]
DELETE /api/v1/school/classes/{id}                               [DIRECTOR]
GET    /api/v1/school/classes/{id}/students                      [DIRECTOR, TEACHER]
```

**Champs SchoolClass :** academicYearId, levelId, name (ex: "6ème A"), option (Scientifique·Littéraire, optionnel), capacity, roomNumber

**Validation :** name unique par année scolaire

---

## F-07 · Matières et affectation enseignants (academic/)

### Matières

```
GET    /api/v1/school/subjects          [ALL_ROLES]
POST   /api/v1/school/subjects          [DIRECTOR]
PUT    /api/v1/school/subjects/{id}     [DIRECTOR]
DELETE /api/v1/school/subjects/{id}     [DIRECTOR]  → isActive=false
```

**Champs Subject :** name (unique), code (ex: "MATH"), color (hex #RRGGBB), isActive

### Affectation matière-classe-enseignant (ClassSubject)

```
GET    /api/v1/school/classes/{classId}/subjects              [ALL_ROLES]
POST   /api/v1/school/classes/{classId}/subjects              [DIRECTOR]
PUT    /api/v1/school/class-subjects/{id}                     [DIRECTOR]
DELETE /api/v1/school/class-subjects/{id}                     [DIRECTOR]
GET    /api/v1/school/teachers/{teacherId}/subjects            [DIRECTOR]
```

**Champs ClassSubject :** classId, subjectId, teacherId (UUID, ref vers User), coefficient (défaut 1, entre 1 et 10), weeklyHours

**Règle :** Un enseignant peut avoir plusieurs ClassSubject. Un ClassSubject unique par (classId + subjectId).

---

## F-08 · Élèves (enrollment/)

```
GET    /api/v1/school/students                [DIRECTOR, TEACHER, ACCOUNTANT]
GET    /api/v1/school/students/{id}           [DIRECTOR, TEACHER, ACCOUNTANT]
POST   /api/v1/school/students                [DIRECTOR, ACCOUNTANT]
PUT    /api/v1/school/students/{id}           [DIRECTOR, ACCOUNTANT]
DELETE /api/v1/school/students/{id}           [DIRECTOR]  → isActive=false
POST   /api/v1/school/students/{id}/photo     [DIRECTOR, ACCOUNTANT]  → upload photo
```

**Champs Student :** studentNumber (auto-généré trigger: `EL-YYYY-NNNN`), firstName, lastName, dateOfBirth, gender (M·F), birthCity, birthCountry (GN défaut), photoUrl, address, parentName, parentPhone (format international +224XXXXXXXXX), medicalNotes, isActive

**Recherche (paramètre `?search=`) :** filtre sur firstName, lastName, studentNumber (LIKE insensible à la casse)

**Validation :**
- firstName + lastName obligatoires
- parentPhone : validation format numéro guinéen (+224 + 9 chiffres) si fourni
- studentNumber généré automatiquement par trigger PostgreSQL, jamais saisi manuellement

**Réponse liste :** `Page<StudentResponse>` triée par lastName ASC par défaut

---

## F-09 · Inscriptions (enrollment/)

```
GET    /api/v1/school/enrollments                             [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/enrollments/{id}                        [ALL_ROLES]
POST   /api/v1/school/enrollments                             [DIRECTOR, ACCOUNTANT]
PUT    /api/v1/school/enrollments/{id}/transfer               [DIRECTOR]
PUT    /api/v1/school/enrollments/{id}/withdraw               [DIRECTOR]
GET    /api/v1/school/classes/{classId}/enrollments           [DIRECTOR, TEACHER]
GET    /api/v1/school/students/{studentId}/enrollments        [ALL_ROLES]
```

**Champs StudentEnrollment :** studentId, classId, academicYearId, enrollmentDate (défaut today), isRepeating, status (ENROLLED·TRANSFERRED·WITHDRAWN·GRADUATED), promotionStatus (PENDING·PROMOTED·REPEATED·GRADUATED), finalAverage

**Règles :**
- UN SEUL enrollment actif par étudiant par année scolaire (contrainte unique `student_id + academic_year_id`)
- À la création : générer automatiquement les `StudentFee` à partir des `FeeStructure` de l'année + classe (appeler `StudentFeeService.generateFeesForEnrollment(enrollment)`)
- Transfert : status → TRANSFERRED + notes de transfert obligatoires
- Retrait : status → WITHDRAWN

**Réponse :** inclut le nom de l'élève, la classe, l'année scolaire

---

## F-10 · Structure des frais (finance/)

```
GET    /api/v1/school/fee-structures                          [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/fee-structures/{id}                     [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/academic-years/{yearId}/fee-structures  [DIRECTOR, ACCOUNTANT]
POST   /api/v1/school/fee-structures                          [DIRECTOR]
PUT    /api/v1/school/fee-structures/{id}                     [DIRECTOR]
DELETE /api/v1/school/fee-structures/{id}                     [DIRECTOR]
```

**Champs FeeStructure :** academicYearId, classId (null = s'applique à toutes les classes), feeType (TUITION·REGISTRATION·CANTEEN·TRANSPORT·EXAM), label (ex: "Frais de scolarité 2024-2025"), amount (Decimal, en GNF), dueDate, installmentsAllowed (bool), maxInstallments (défaut 3)

**Règle d'unicité :** une seule FeeStructure par (academicYearId + classId + feeType)

**Note :** si classId=null, la structure s'applique à TOUTES les classes de l'année (frais généraux)

---

## F-11 · Frais élèves et suivi (finance/)

```
GET    /api/v1/school/students/{studentId}/fees               [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/students/{studentId}/fees/summary       [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/enrollments/{enrollmentId}/fees         [DIRECTOR, ACCOUNTANT]
PUT    /api/v1/school/student-fees/{id}/discount              [DIRECTOR]
POST   /api/v1/school/enrollments/{enrollmentId}/generate-fees [DIRECTOR]
GET    /api/v1/school/fees/unpaid                             [DIRECTOR, ACCOUNTANT]
```

**Champs StudentFee :** enrollmentId, feeStructureId, amountDue, amountPaid (mis à jour par trigger), discountAmount, discountReason, dueDate (copié depuis FeeStructure), status (UNPAID·PARTIAL·PAID·OVERDUE·WAIVED), lastReminderSentAt

**Génération automatique (generateFeesForEnrollment) :**
- Charger toutes les `FeeStructure` de l'année de l'enrollment
- Filtrer : classId=null OU classId=enrollment.classId
- Créer un `StudentFee` par FeeStructure avec amountDue=feeStructure.amount, amountPaid=0, status=UNPAID

**Mise à jour du statut (trigger PostgreSQL ou service) après chaque PaymentAllocation :**
- remaining = amountDue - discountAmount - amountPaid
- Si remaining ≤ 0 → status=PAID
- Si amountPaid > 0 AND remaining > 0 → status=PARTIAL
- Si status IN (UNPAID, PARTIAL) AND dueDate < today → status=OVERDUE

**Summary étudiant (PaymentSummaryResponse) :**
```json
{
  "studentId": "uuid",
  "totalDue": 450000,
  "totalPaid": 200000,
  "totalRemaining": 250000,
  "collectionRatePct": 44.4,
  "unpaidFeesCount": 2,
  "overdueFeesCount": 1
}
```

---

## F-12 · Paiements (finance/)

```
POST   /api/v1/school/payments                              [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/payments                              [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/payments/{id}                         [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/payments/student/{studentId}          [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/payments/student/{studentId}/pending-fees [DIRECTOR, ACCOUNTANT]
DELETE /api/v1/school/payments/{id}?reason=...              [DIRECTOR]  → annulation dans les 24h
```

**Request création :**
```json
{
  "studentId": "uuid",
  "amount": 200000,
  "paymentMethod": "orange_money",
  "referenceNumber": "CM2024XXXXX",
  "paymentDate": "2024-11-15",
  "notes": "Paiement partiel T1",
  "allocations": [
    { "studentFeeId": "uuid", "amount": 150000 },
    { "studentFeeId": "uuid", "amount": 50000 }
  ]
}
```

**Logique PaymentService.createPayment :**
1. Vérifier que la somme des allocations = amount total → 400 ALLOCATION_MISMATCH si différent
2. Pour chaque allocation : vérifier que le StudentFee existe et n'est pas PAID → 400 FEE_ALREADY_PAID
3. Vérifier que amountAllocated ≤ remaining dû par frais → 400 OVER_PAYMENT
4. Créer le `Payment` (receiptNumber généré par trigger PostgreSQL : `REC-YYYYMM-NNNN`)
5. Créer les `PaymentAllocation` (une par frais)
6. Trigger PostgreSQL recalcule automatiquement `StudentFee.amountPaid` et `status`
7. Calculer le solde restant total de l'élève
8. Envoyer SMS de confirmation au parent (si parentPhone renseigné) via template `payment_received`
9. Retourner `PaymentResponse` avec receiptNumber, allocations, totalRemainingAfter

**Filtres GET /payments :**
- `?from=2024-01-01&to=2024-12-31` (dates)
- `?method=orange_money`
- `?studentId=uuid`

**Annulation (DELETE) :**
- Vérifier que paymentDate = aujourd'hui ou hier → 400 CANCELLATION_WINDOW_EXPIRED si plus ancien
- Ne pas supprimer : mettre une note "[ANNULÉ par userId le date] - Motif: ..."
- Phase 2 : créer un avoir négatif et recalculer les StudentFee

**PaymentResponse :**
```json
{
  "id": "uuid",
  "studentId": "uuid",
  "studentName": "Bah Mariama",
  "amount": 200000,
  "paymentDate": "2024-11-15",
  "paymentMethod": "orange_money",
  "paymentMethodLabel": "Orange Money",
  "referenceNumber": "CM2024XXXXX",
  "receiptNumber": "REC-202411-0042",
  "totalRemainingAfter": 250000,
  "allocations": [
    {
      "studentFeeId": "uuid",
      "feeLabel": "Frais de scolarité T1",
      "amountAllocated": 150000,
      "newBalance": 0
    }
  ]
}
```

---

## F-13 · SMS (communication/)

### Templates (pré-chargés à l'onboarding)

| code | category | template |
|------|----------|----------|
| `fee_reminder` | FINANCIAL | `Bonjour {{parent_name}}, les frais de {{student_name}} ({{class_name}}) s'élèvent à {{amount_due}} GNF. Merci de régler avant le {{due_date}}.` |
| `payment_received` | FINANCIAL | `Bonjour {{parent_name}}, paiement de {{amount_paid}} GNF reçu pour {{student_name}}. Reste dû: {{remaining}} GNF. Reçu n°{{receipt_number}}.` |
| `report_card_published` | ACADEMIC | `Bonjour {{parent_name}}, le bulletin de {{term_name}} de {{student_name}} est disponible. Moy: {{average}}/20. Rang: {{rank}}/{{class_size}}.` |
| `absence_notification` | ACADEMIC | `Bonjour {{parent_name}}, votre enfant {{student_name}} était absent(e) le {{date}}. Merci de nous contacter.` |

```
GET    /api/v1/school/sms/templates                    [DIRECTOR]
POST   /api/v1/school/sms/templates                    [DIRECTOR]
PUT    /api/v1/school/sms/templates/{id}               [DIRECTOR]
GET    /api/v1/school/sms/logs                         [DIRECTOR, ACCOUNTANT]
GET    /api/v1/school/sms/logs?studentId=uuid          [DIRECTOR, ACCOUNTANT]
POST   /api/v1/school/sms/fee-reminder/{studentFeeId}  [DIRECTOR, ACCOUNTANT]
POST   /api/v1/school/sms/bulk-reminders               [DIRECTOR]   → async
GET    /api/v1/school/sms/stats                        [DIRECTOR]
```

**SmsTemplateEngine :** résoudre les variables `{{var}}` avec une Map<String,String>
**SmsProvider (interface) :** `send(String to, String message): SmsResult`
- `LoggingSmsProvider` : log en console, retourner un fakeId (pour dev)
- `OrangeSmsProvider` : OAuth2 token (cache 90 jours), normalisation numéros +224
- `TwilioSmsProvider` : fallback international
- Sélection via `app.sms.provider=logging|orange|twilio` (application.yml)

**SmsLog :** enregistrer TOUS les SMS (tentatives incluses) avec provider, status (PENDING·SENT·DELIVERED·FAILED), errorMessage, sentAt, deliveredAt

**Rappels automatiques (SmsScheduler) :**
```
@Scheduled(cron = "0 0 8 * * MON-FRI")   → sendBulkFeeReminders()
@Scheduled(cron = "0 0 0 * * *")         → markOverdueFees()
@Scheduled(cron = "0 */15 * * * *")      → refreshDashboardMaterializedView()
```

**sendBulkFeeReminders :** Pour chaque tenant actif → charger StudentFee avec status IN (UNPAID, PARTIAL, OVERDUE) ET (lastReminderSentAt IS NULL OR lastReminderSentAt < now()-7j) → envoyer SMS template fee_reminder → mettre à jour lastReminderSentAt

---

## F-14 · Dashboard (dashboard/)

```
GET /api/v1/school/dashboard/stats    [DIRECTOR, ACCOUNTANT]
```

**Logique :** lire depuis la vue matérialisée PostgreSQL `mv_dashboard_stats` (réponse immédiate)
**Cache Caffeine :** `@Cacheable("dashboard_stats")` TTL 15 minutes
**Refresh :** scheduler toutes les 15 minutes + `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_dashboard_stats`

**DashboardStatsResponse :**
```json
{
  "totalStudents": 245,
  "activeStudents": 238,
  "maleStudents": 128,
  "femaleStudents": 110,
  "totalFeesExpected": 110250000,
  "totalFeesCollected": 73500000,
  "collectionRatePct": 66.7,
  "studentsWithDebt": 87,
  "studentsOverdue": 23,
  "pendingGradeEntries": 12,
  "smsToday": 15,
  "smsThisMonth": 203
}
```

---

# PHASE 2 — Notes, bulletins, présences

---

## F-15 · Saisie des notes (grading/)

```
GET    /api/v1/school/grades?classId=&subjectId=&termId=    [DIRECTOR, TEACHER]
GET    /api/v1/school/grades/enrollment/{enrollmentId}?termId= [DIRECTOR, TEACHER]
POST   /api/v1/school/grades                                 [DIRECTOR, TEACHER]
POST   /api/v1/school/grades/bulk                            [DIRECTOR, TEACHER]  → saisie en masse pour une classe
PUT    /api/v1/school/grades/{id}                            [DIRECTOR, TEACHER]
DELETE /api/v1/school/grades/{id}                            [DIRECTOR]
```

**Champs Grade :** enrollmentId, classSubjectId, termId, value (Decimal 0-20), evaluationType (DEVOIR·COMPOSITION·ORAL·TP), evaluationLabel ("Devoir 1"), evaluationDate, enteredBy (userId courant), comment

**Règles :**
- `gradesEntryOpen=true` sur le Term → sinon 400 GRADES_ENTRY_CLOSED
- Un TEACHER peut saisir uniquement les notes des ClassSubject où il est affecté (teacherId correspond à l'utilisateur courant)
- Un DIRECTOR peut saisir toutes les notes
- Pas de doublons : (enrollmentId + classSubjectId + termId + evaluationLabel) unique → 409 DUPLICATE_GRADE
- La value doit être entre 0 et la note max du ClassSubject (défaut 20)

**Bulk entry request :**
```json
{
  "classSubjectId": "uuid",
  "termId": "uuid",
  "evaluationType": "DEVOIR",
  "evaluationLabel": "Devoir 1",
  "evaluationDate": "2024-10-15",
  "grades": [
    { "enrollmentId": "uuid", "value": 14.5, "comment": "" },
    { "enrollmentId": "uuid", "value": 11.0, "comment": "" }
  ]
}
```

**Calcul des moyennes (trigger PostgreSQL ou service) :**
Après chaque INSERT/UPDATE sur `grades` :
- Calculer la moyenne pondérée par (coefficient × valeur) pour chaque ClassSubject du trimestre
- Stocker dans une table intermédiaire ou calculer à la volée lors de la génération du bulletin

---

## F-16 · Bulletins scolaires (grading/)

```
GET    /api/v1/school/report-cards?termId=&classId=          [DIRECTOR, TEACHER]
GET    /api/v1/school/report-cards/{id}                      [DIRECTOR, TEACHER, ACCOUNTANT]
GET    /api/v1/school/report-cards/enrollment/{enrollmentId}/term/{termId} [ALL_ROLES]
POST   /api/v1/school/report-cards/generate                  [DIRECTOR]   → génère pour une classe entière
PUT    /api/v1/school/report-cards/{id}/comments             [DIRECTOR, TEACHER]
POST   /api/v1/school/report-cards/{id}/publish              [DIRECTOR]
GET    /api/v1/school/report-cards/{id}/pdf                  [ALL_ROLES]  → téléchargement PDF
POST   /api/v1/school/report-cards/class/{classId}/term/{termId}/publish-all [DIRECTOR]
```

**Champs ReportCard :** enrollmentId, termId, generalAverage, rankInClass, classSize, teacherComment, directorComment, status (DRAFT·PUBLISHED·SENT_TO_PARENT), pdfUrl, publishedAt

**Génération (ReportCardService.generateForClass) :**
1. Charger tous les enrollments ENROLLED de la classe
2. Pour chaque enrollment :
   a. Charger toutes les notes du trimestre (jointure classSubject pour le coefficient)
   b. `generalAverage = Σ(average_per_subject × coefficient) / Σ(coefficients)`
   c. Créer ou mettre à jour le ReportCard avec status=DRAFT
3. Calculer le classement (`rankInClass`) pour tous les élèves de la classe (tri par generalAverage DESC)

**Publication (ReportCardService.publish) :**
1. Vérifier que generalAverage est calculé
2. status → PUBLISHED, publishedAt = now()
3. Générer le PDF (Thymeleaf template HTML → OpenHTMLtoPDF)
4. Sauvegarder le PDF (StorageService : local ou S3)
5. Mettre à jour pdfUrl
6. Envoyer SMS au parent via template `report_card_published`
7. status → SENT_TO_PARENT

**PDF (infrastructure/pdf) :**
- Template Thymeleaf : `src/main/resources/templates/report-card.html`
- Contenu : logo école, nom élève, classe, trimestre, tableau matières (notes + moyennes + coefficients), moyenne générale, rang, appréciations
- Retourner le PDF en streaming via `ResponseEntity<byte[]>` avec `Content-Type: application/pdf`

---

## F-17 · Présences (attendance/)

```
POST   /api/v1/school/attendance                              [DIRECTOR, TEACHER]
POST   /api/v1/school/attendance/bulk                         [DIRECTOR, TEACHER]  → appel d'une classe entière
GET    /api/v1/school/attendance?enrollmentId=&from=&to=      [DIRECTOR, TEACHER]
GET    /api/v1/school/attendance/class/{classId}?date=        [DIRECTOR, TEACHER]
GET    /api/v1/school/attendance/student/{studentId}/summary?termId= [ALL_ROLES]
PUT    /api/v1/school/attendance/{id}                         [DIRECTOR, TEACHER]  → justifier ou corriger
```

**Champs Attendance :** enrollmentId, date, period (FULL_DAY·MORNING·AFTERNOON, défaut FULL_DAY), status (PRESENT·ABSENT·LATE·EXCUSED), justification, recordedBy (userId courant)

**Contrainte unique :** (enrollmentId + date + period) → 409 ATTENDANCE_ALREADY_RECORDED si doublon

**Bulk attendance request :**
```json
{
  "classId": "uuid",
  "date": "2024-11-15",
  "period": "FULL_DAY",
  "records": [
    { "enrollmentId": "uuid", "status": "PRESENT" },
    { "enrollmentId": "uuid", "status": "ABSENT" },
    { "enrollmentId": "uuid", "status": "LATE",  "justification": "Transport" }
  ]
}
```

**Règle SMS absence :**
Après un `status=ABSENT` (pas EXCUSED) : vérifier si un SMS absence a déjà été envoyé aujourd'hui pour cet enrollment → si non, envoyer SMS template `absence_notification` au parentPhone de l'élève

**Résumé absences (AttendanceSummaryResponse) :**
```json
{
  "enrollmentId": "uuid",
  "studentName": "Bah Mariama",
  "termId": "uuid",
  "totalAbsences": 5,
  "justifiedAbsences": 2,
  "unjustifiedAbsences": 3,
  "lateCount": 4
}
```

---

# PHASE 3 — Emploi du temps, avancement élèves

---

## F-18 · Emploi du temps (timetable/)

```
GET    /api/v1/school/time-slots                              [ALL_ROLES]
POST   /api/v1/school/time-slots                              [DIRECTOR]
PUT    /api/v1/school/time-slots/{id}                         [DIRECTOR]
DELETE /api/v1/school/time-slots/{id}                         [DIRECTOR]

GET    /api/v1/school/timetable/class/{classId}?yearId=       [ALL_ROLES]   → grille semaine
GET    /api/v1/school/timetable/teacher/{teacherId}?yearId=   [DIRECTOR, TEACHER]
POST   /api/v1/school/timetable                               [DIRECTOR]
PUT    /api/v1/school/timetable/{id}                          [DIRECTOR]
DELETE /api/v1/school/timetable/{id}                          [DIRECTOR]
```

**Champs TimeSlot :** dayOfWeek (MONDAY..SATURDAY), startTime, endTime, label ("Heure 1"), orderIndex

**Champs TimetableEntry :** classSubjectId, timeSlotId, academicYearId, termId (null = toute l'année), roomNumber, isActive

**Détection conflits (TimetableService.validateNoConflict) :**
- Même classe + même timeSlot → 409 CLASS_TIMESLOT_CONFLICT
- Même enseignant + même timeSlot → 409 TEACHER_TIMESLOT_CONFLICT

**Réponse grille hebdomadaire (WeeklyTimetableResponse) :**
```json
{
  "classId": "uuid",
  "className": "4ème B",
  "entries": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "08:00",
      "endTime": "09:00",
      "subjectName": "Mathématiques",
      "teacherName": "M. Diallo",
      "roomNumber": "Salle A"
    }
  ]
}
```

---

## F-19 · Avancement des élèves en fin d'année (enrollment/)

```
GET    /api/v1/school/promotions?yearId=&classId=         [DIRECTOR]   → propositions
POST   /api/v1/school/promotions/{enrollmentId}/validate   [DIRECTOR]   → valider le passage
POST   /api/v1/school/promotions/{enrollmentId}/override   [DIRECTOR]   → forcer une décision
POST   /api/v1/school/promotions/process-class/{classId}  [DIRECTOR]   → traiter toute la classe
```

**Règles PromotionService :**
- Calcul de la `finalAverage` depuis les 3 trimestres (moyenne des moyennes générales)
- Si `finalAverage >= Grade.passingAverage` → promotionStatus=PROMOTED → créer enrollment dans la classe suivante pour la nouvelle année
- Si `finalAverage < Grade.passingAverage` → promotionStatus=REPEATED → recréer enrollment dans la même classe pour la nouvelle année
- Si dernière classe d'un niveau → promotionStatus=GRADUATED
- Le DIRECTOR peut toujours `override` la décision automatique

---

# EXIGENCES TRANSVERSALES

---

## TX-01 · Multitenancy

- `TenantContext` (ThreadLocal) : setter dans `JwtAuthenticationFilter` depuis le claim `tenantId` du JWT
- `TenantIdentifierResolver` (Hibernate) : retourner `TenantContext.get()` ou "public" si null
- `SchemaMultiTenantConnectionProvider` : `SET search_path = {schemaName}, public` sur chaque connexion
- Validation du schemaName : regex `^[a-zA-Z0-9_]+$` avant exécution SQL
- Nettoyage MDC : `TenantContext.clear()` dans le finally du filtre

## TX-02 · Sécurité

**Rôles et accès :**
| Route | DIRECTOR | TEACHER | ACCOUNTANT |
|-------|----------|---------|------------|
| Gestion users, classes, matières | ✅ | ❌ | ❌ |
| Notes, bulletins | ✅ | ✅ | ❌ |
| Paiements, frais | ✅ | ❌ | ✅ |
| Consultation (élèves, dashboard) | ✅ | ✅ | ✅ |
| Présences | ✅ | ✅ | ❌ |
| DELETE (tout) | ✅ | ❌ | ❌ |

- `@PreAuthorize` sur chaque méthode controller
- `AuthenticatedUser` (Principal) récupéré via `@AuthenticationPrincipal` dans les controllers
- CORS : allow `http://localhost:*` en dev, URL configurée en prod

## TX-03 · Migrations Flyway

```
src/main/resources/db/migration/
├── public/
│   └── V1__init_public_schema.sql     → tables: schools, subscription_plans, school_subscriptions, subscription_payments
└── tenant/
    └── V1__init_tenant_schema.sql     → toutes les tables métier + triggers + vue matérialisée
```

**Triggers PostgreSQL dans V1__init_tenant_schema.sql :**
- `fn_generate_student_number()` : génère `EL-YYYY-NNNN` sur INSERT students
- `fn_generate_receipt_number()` : génère `REC-YYYYMM-NNNN` sur INSERT payments
- `fn_recalculate_fee_status()` : recalcule StudentFee.status après INSERT payment_allocations
- `fn_update_updated_at()` : met à jour updated_at sur toutes les tables concernées

**Vue matérialisée :**
```sql
CREATE MATERIALIZED VIEW mv_dashboard_stats AS
-- stats agrégées depuis enrollments, student_fees, sms_logs
-- (voir diagramme de classes pour le détail des champs)
```

## TX-04 · Stockage fichiers (infrastructure/storage)

- Interface `StorageService` : `store(MultipartFile, folder): String url` et `delete(String url)`
- `LocalStorageService` : sauvegarde dans `${app.storage.local.base-path}/{folder}/{uuid}.{ext}`
- URLs retournées : `/uploads/{folder}/{filename}` servi par Spring Boot StaticResource
- Utilisé pour : photos élèves, PDFs bulletins, PDFs reçus

## TX-05 · Métriques Micrometer

Compteurs à exposer (`/actuator/metrics`) :
- `schoolsaas.payments.success` (tag: method)
- `schoolsaas.payments.failed`
- `schoolsaas.sms.sent` (tags: provider, status, category)
- `schoolsaas.reportcard.published`
- `schoolsaas.students.active` (Gauge)

## TX-06 · Variables d'environnement requises

```
JWT_SECRET                → chaîne min 32 chars pour HMAC-SHA256
DATABASE_URL              → jdbc:postgresql://host:5432/school_saas
DATABASE_USERNAME
DATABASE_PASSWORD
SMS_PROVIDER              → logging|orange|twilio
ORANGE_CLIENT_ID          → (si provider=orange)
ORANGE_CLIENT_SECRET      → (si provider=orange)
STORAGE_PROVIDER          → local|s3
LOCAL_STORAGE_PATH        → /var/schoolsaas/uploads (si local)
FRONTEND_URL              → pour CORS
```

---

# RÉSUMÉ PRIORISÉ

| Phase | ID | Fonctionnalité | Complexité |
|-------|----|----------------|------------|
| **MVP** | F-01 | Onboarding école (création tenant) | Haute |
| **MVP** | F-02 | Authentification JWT | Moyenne |
| **MVP** | F-03 | Gestion utilisateurs | Faible |
| **MVP** | F-04 | Années scolaires | Faible |
| **MVP** | F-05 | Trimestres | Faible |
| **MVP** | F-06 | Niveaux et classes | Faible |
| **MVP** | F-07 | Matières et affectation profs | Faible |
| **MVP** | F-08 | Élèves (CRUD) | Faible |
| **MVP** | F-09 | Inscriptions + génération frais auto | Moyenne |
| **MVP** | F-10 | Structure des frais | Faible |
| **MVP** | F-11 | Suivi frais élèves | Moyenne |
| **MVP** | F-12 | Enregistrement paiements | Haute |
| **MVP** | F-13 | SMS (templates + envoi + scheduler) | Haute |
| **MVP** | F-14 | Dashboard statistiques | Moyenne |
| **Phase 2** | F-15 | Saisie des notes | Moyenne |
| **Phase 2** | F-16 | Bulletins PDF | Haute |
| **Phase 2** | F-17 | Présences + SMS absence | Moyenne |
| **Phase 3** | F-18 | Emploi du temps | Moyenne |
| **Phase 3** | F-19 | Avancement élèves fin d'année | Haute |


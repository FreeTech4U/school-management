# 🔍 ANALYSE COMPLÈTE : SPECS vs IMPLÉMENTATION

## 📊 RÉSUMÉ EXÉCUTIF
- **27 entités requises** vs **20 implémentées** → 74% ✓
- **~60 endpoints requis** vs **~25 implémentés** → 42% ✓
- **Score global : 5.4/10** → MVP INCOMPLET, needs Phase 2/3

---

## 1️⃣ CHAMPS MANQUANTS DANS LES ENTITÉS

### 🔴 CRITICAL - Bloque MVP

#### **FeeStructure.feeType** ❌ ABSENT
- Specs F-10 exige : `TUITION|REGISTRATION|CANTEEN|TRANSPORT|EXAM`
- **Impact :** Impossible de différencier les frais
- **Fix :** Ajouter champ + Enum FeeType

```java
@Column(name = "fee_type")
@Enumerated(EnumType.STRING)
private FeeType feeType; // TUITION, REGISTRATION, CANTEEN, TRANSPORT, EXAM
```

---

#### **Payment.status** ❌ ABSENT
- Specs exige : `PENDING|CONFIRMED|FAILED|CANCELLED`
- **Implémentation :** Payment n'a pas de status du tout
- **Impact :** Impossible de tracker paiements échoués
- **Fix :** Ajouter champ + Enum PaymentStatus

```java
@Column(nullable = false)
@Enumerated(EnumType.STRING)
private PaymentStatus status = PaymentStatus.CONFIRMED; // Default
```

---

### 🟠 HIGH - À faire avant prod

#### **ClassSubject** INCOMPLET
Manquent :
- `coefficient` (1-10, défaut 1) → NÉCESSAIRE pour calcul moyennes
- `weeklyHours` (pour planification)

```java
@Column(name = "coefficient", nullable = false)
private Integer coefficient = 1;

@Column(name = "weekly_hours")
private Integer weeklyHours;
```

---

#### **FeeStructure** INCOMPLET
Manquent :
- `installmentsAllowed` (Boolean)
- `maxInstallments` (défaut 3)

```java
@Column(name = "installments_allowed", nullable = false)
private Boolean installmentsAllowed = true;

@Column(name = "max_installments", nullable = false)
private Integer maxInstallments = 3;
```

---

### 🟡 MEDIUM - Phase 2

#### **StudentEnrollment.transferNotes** ❌
```java
@Column(name = "transfer_notes")
private String transferNotes;
```

#### **SmsTemplate** - Pré-chargés manquants ❌
Specs exige 4 templates à l'onboarding :
- `fee_reminder`
- `payment_received`
- `report_card_published`
- `absence_notification`

**Fix :** Créer données de seed dans V2__insert_templates.sql

---

## 2️⃣ ÉNUMÉRATIONS MANQUANTES PARTOUT

Au lieu de `String` (❌ DANGER de typos), utiliser `Enum` (✅ Sécurisé à la compile) :

| Où ? | Enum requis | État |
|------|-------------|------|
| **StudentEnrollment** | `EnrollmentStatus`, `PromotionStatus` | ❌ String |
| **StudentFee** | `FeeStatus` | ❌ String |
| **Payment** | `PaymentMethod`, `PaymentStatus` | ⚠️ PaymentMethod String |
| **Grade** | `EvaluationType` | ❌ String |
| **Attendance** | `Period`, `AttendanceStatus` | ❌ String |
| **SmsLog** | `SmsStatus` | ❌ String |
| **ReportCard** | `ReportCardStatus` | ❌ String |
| **FeeStructure** | `FeeType` | ❌ ABSENT |

**Action :** Créer `src/main/java/com/schoolsaas/common/enums/` avec tous les enums.

---

## 3️⃣ RELATIONS HIBERNAT MANQUANTES

Actuellement : UUIDs bruts (❌ pas de jointures possibles)
Requis : Annotations `@ManyToOne` (✅ Lazy loading, Cascade)

### À ajouter :

```java
// ClassSubject → User (teacher)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "teacher_id", nullable = false)
private User teacher;

// StudentFee → StudentEnrollment
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "enrollment_id", nullable = false)
private StudentEnrollment enrollment;

// Grade → StudentEnrollment, ClassSubject, Term
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "enrollment_id", nullable = false)
private StudentEnrollment enrollment;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "class_subject_id", nullable = false)
private ClassSubject classSubject;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "term_id", nullable = false)
private Term term;

// Attendance → StudentEnrollment
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "enrollment_id", nullable = false)
private StudentEnrollment enrollment;

// PaymentAllocation → Payment & StudentFee
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "payment_id", nullable = false)
private Payment payment;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "student_fee_id", nullable = false)
private StudentFee studentFee;
```

**Impact :** Élimine N+1 queries et permet jointures optimisées.

---

## 4️⃣ ENDPOINTS MANQUANTS (42% implémentés)

### 🔴 CRITICAL (0 endpoints)

| Feature | Endpoints requis | Manquant | Impact |
|---------|------------------|----------|--------|
| **F-11 : Suivi frais** | 6 | 6 ❌ | Impossible consulter frais élèves |
| **F-16 : Bulletins PDF** | 8 | 8 ❌ | Impossible générer bulletins |
| **F-19 : Promotion** | 4 | 4 ❌ | Impossible avancer élèves |

### 🟠 HIGH (Partiels)

| Feature | Total | Implémentés | % |
|---------|-------|-------------|---|
| **F-10 : Structures frais** | 4 | 2 | 50% |
| **F-12 : Paiements** | 6 | 1 | 17% |
| **F-13 : SMS** | 7 | 2 | 29% |
| **F-15 : Saisie notes** | 6 | 2 | 33% |
| **F-17 : Présences** | 6 | 1 | 17% |
| **F-18 : Timetable** | 9 | 1 | 11% |
| **F-09 : Inscriptions** | 5 | 2 | 40% |

### Endpoints manquants clés :

```
# F-11 Frais (0/6)
❌ GET    /api/v1/school/students/{studentId}/fees
❌ GET    /api/v1/school/students/{studentId}/fees/summary
❌ GET    /api/v1/school/enrollments/{enrollmentId}/fees
❌ PUT    /api/v1/school/student-fees/{id}/discount
❌ POST   /api/v1/school/enrollments/{enrollmentId}/generate-fees
❌ GET    /api/v1/school/fees/unpaid

# F-16 Bulletins (0/8)
❌ GET    /api/v1/school/report-cards
❌ POST   /api/v1/school/report-cards/generate
❌ POST   /api/v1/school/report-cards/{id}/publish
❌ GET    /api/v1/school/report-cards/{id}/pdf
... (4 autres)

# F-19 Promotion (0/4)
❌ GET    /api/v1/school/promotions
❌ POST   /api/v1/school/promotions/{enrollmentId}/validate
❌ POST   /api/v1/school/promotions/{enrollmentId}/override
❌ POST   /api/v1/school/promotions/process-class/{classId}
```

---

## 5️⃣ SERVICES MÉTIER MANQUANTS

### 🔴 CRITICAL

| Service | Méthodes requises | État |
|---------|-------------------|------|
| **ReportCardService** | generateForClass(), publish(), generatePdf() | ❌ N'existe pas |
| **PromotionService** | calculatePromotionStatus(), processClass() | ❌ N'existe pas |
| **RankingService** | calculateRankings() | ❌ N'existe pas |

### 🟠 HIGH

| Service | Méthode | État |
|---------|---------|------|
| **StudentFeeService** | generateFeesForEnrollment() | ⚠️ Exists mais pas appelée auto |
| **AttendanceService** | sendAbsenceSms() | ❌ Manquant |
| **SmsTemplateEngine** | resolveTemplate() | ❌ Manquant |
| **SmsScheduler** | sendBulkReminders(), markOverdue() | ❌ N'existe pas |

**Impact :** Phase 2/3 complètement bloquée.

---

## 6️⃣ MAPPERS DTO MANQUANTS (15% des mappers)

Actuellement retournent les **entités JPA directes** (❌ Mauvais) :
Devraient retourner des **DTOs** (✅ Bon) via **MapStruct** :

Mappers manquants :
```
❌ AcademicYear ↔ AcademicYearResponse
❌ Term ↔ TermResponse
❌ StudentFee ↔ StudentFeeResponse
❌ FeeStructure ↔ FeeStructureResponse
❌ Payment ↔ PaymentResponse
❌ Grade ↔ GradeResponse
❌ SmsTemplate ↔ SmsTemplateResponse
❌ Attendance ↔ AttendanceResponse
❌ ReportCard ↔ ReportCardResponse
```

**Impact :** Sécurité + Flex API (impossible modifier réponses sans toucher entités).

---

## 7️⃣ BASE DE DONNÉES : TRIGGERS & VUES

### ✅ Triggers PostgreSQL IMPLÉMENTÉS

**Specs (TX-03) exige :** ✅ **TOUS PRÉSENTS** (V1__init_tenant_schema.sql)

1. **fn_update_updated_at()** ✅
   - Défini ligne 14-20
   - Triggers appliqués sur 17 tables (ligne 495-546)
   - Mise à jour automatique des `updated_at`

2. **fn_generate_student_number()** ✅
   - Défini ligne 558-575
   - Génère format `EL-YYYY-NNNN` ✅
   - Trigger trg_generate_student_number ligne 577

3. **fn_generate_receipt_number()** ✅
   - Défini ligne 589-605
   - Génère format `REC-YYYYMM-NNNN` ✅
   - Trigger trg_generate_receipt_number ligne 607

4. **fn_recalculate_fee_status()** ✅
   - Défini ligne 620-666
   - Recalcule StudentFee.status après PaymentAllocation
   - Gère INSERT/UPDATE/DELETE
   - Trigger trg_recalculate_fee_status ligne 668

**Impact :** Numéros générés AUTOMATIQUEMENT ✅

---

### ✅ Vue matérialisée IMPLÉMENTÉE

**Specs (F-14) exige :** ✅ **PRÉSENT** (V1__init_tenant_schema.sql)

- Créée ligne 679-706 : `mv_dashboard_stats`
- Index UNIQUE pour REFRESH CONCURRENTLY (ligne 709)
- Calcule tous les KPIs dashboard
- À rafraîchir par scheduler toutes les 15 min

---

### ✅ Templates SMS PRÉ-CHARGÉS IMPLÉMENTÉS

**Specs (F-13) exige 4 templates :** ✅ **TOUS PRÉSENTS** (V1__init_tenant_schema.sql)

Insérés ligne 723-738 :
- ✅ `fee_reminder` (FINANCIAL)
- ✅ `payment_received` (FINANCIAL)
- ✅ `report_card_published` (ACADEMIC)
- ✅ `absence_notification` (ACADEMIC)

Chaque template inclut les variables en JSON ✅

**Impact :** SMS prêts à être envoyés immédiatement ✅

---

## 8️⃣ VALIDATIONS MANQUANTES

### ❌ Format numéro guinéen
Specs F-08 : `parentPhone` doit être `+224XXXXXXXXX` (9 chiffres)
```java
// Manquant dans Student entity :
if (!parentPhone.matches("^\\+224\\d{9}$")) 
    throw new BusinessException("INVALID_PHONE");
```

### ❌ Validation slug
Specs F-01 : slug = lowercase + tirets, unique
```java
if (!slug.matches("^[a-z0-9-]+$")) 
    throw new BusinessException("INVALID_SLUG");
```

### ❌ Coefficient ClassSubject
Specs F-07 : coefficient entre 1 et 10
```java
if (coefficient < 1 || coefficient > 10) 
    throw new BusinessException("COEFFICIENT_OUT_OF_RANGE");
```

### ❌ Rate limiting
Specs TX-02 : endpoints publics doivent être rate-limited
```java
// Manquant :
@RateLimit(value = 5, timeUnit = "MINUTE")
public ResponseEntity login(...) { ... }
```

---

## 9️⃣ TESTS CASSÉS (87% passing, 13% errors)

### Problèmes :

```
❌ SchoolSaasApplicationTests.contextLoads
   → JWT_SECRET manquant en env de test

❌ OpenApiGeneratorTest
   → ApplicationContext ne charge pas avec multi-tenancy

❌ DashboardServiceTest (2 tests)
   → TenantContext vide

❌ MultiTenancyIntegrationTest
   → Database réelle non disponible
```

### Fix :<br>
1. Utiliser `Testcontainers` pour PostgreSQL réelle
2. Ajouter `@BeforeEach` pour setter TenantContext dans les tests
3. Configurer JWT_SECRET dans `application-test.properties`

---

## 🔟 SCORECARD PAR DOMAINE

| Domaine | Entités | Endpoints | Services | **Score** |
|---------|---------|-----------|----------|-----------|
| Platform | ✅ 4/4 | ✅ 2/2 | ✅ 2/2 | **10/10** |
| Identity | ✅ 2/2 | ✅ 5/5 | ✅ 2/2 | **10/10** |
| Academic | ⚠️ 5/6 | ✅ 10/10 | ✅ 3/3 | **9/10** |
| Enrollment | ✅ 2/2 | ⚠️ 2/5 | ⚠️ 2/3 | **6/10** |
| **Finance** | ⚠️ 4/5 | ❌ 1/11 | ⚠️ 2/3 | **2/10** |
| Grading | ✅ 2/2 | ❌ 2/6 | ❌ 0/4 | **2/10** |
| Attendance | ✅ 1/1 | ❌ 1/6 | ⚠️ 1/1 | **3/10** |
| Timetable | ✅ 2/2 | ❌ 1/9 | ⚠️ 1/1 | **3/10** |
| Communication | ⚠️ 1/2 | ⚠️ 2/7 | ⚠️ 1/2 | **3/10** |
| Dashboard | ✅ 0/0 | ✅ 1/1 | ⚠️ 1/1 | **6/10** |

**Moyenne : 5.4/10** ← MVP INCOMPLET

---

## 📋 ACTION PLAN PRIORISÉ

### 🔴 P0 (Bloque MVP)
1. [ ] Ajouter `FeeStructure.feeType` + Enum
2. [ ] Ajouter `Payment.status` + Enum
3. [ ] Implémenter ReportCardService (generate + publish + PDF)
4. [ ] Implémenter PromotionService
5. [ ] Créer 6 endpoints F-11 (Frais)
6. [ ] Créer 8 endpoints F-16 (Bulletins)
7. [ ] Créer 4 endpoints F-19 (Promotion)
8. [ ] ✅ Triggers PostgreSQL (déjà implémentés)

### 🟠 P1 (Avant prod)
1. [ ] Convertir TOUS les String → Enum (8 enums)
2. [ ] Ajouter @ManyToOne relations Hibernate
3. [ ] Créer tous les Mappers DTOs
4. [ ] Implémenter SmsScheduler
5. [ ] ✅ Vue matérialisée dashboard (déjà implémentée)
6. [ ] Ajouter validations (phone, slug, coefficient)
7. [ ] Fixer tests cassés
8. [ ] Ajouter rate-limiting endpoints publics

### 🟡 P2 (Phase 2/3)
1. [ ] Implémenter endpoints F-15 (Saisie notes)
2. [ ] Implémenter endpoints F-18 (Timetable)
3. [ ] Implémenter endpoints F-17 (Présences)
4. [ ] Ajouter ClassSubject.coefficient + weeklyHours
5. [ ] Ajouter SmsTemplateEngine variable resolution
6. [ ] Audit logs sur Grade/Payment/etc

---

## 📊 CHIFFRES CLÉS

| Métrique | Réalité | % |
|----------|---------|---|
| Entités complètes | 14/27 | 52% |
| Endpoints implémentés | 25/60 | 42% |
| Enums définis | 0/8 | 0% ⚠️ |
| Mappers créés | 3/20 | 15% ⚠️ |
| Relations @ManyToOne | 2/8 | 25% ⚠️ |
| Tests passants | 33/38 | 87% ⚠️ |
| Services métier | 15/25 | 60% |
| Triggers DB | 4/4 | **100% ✅** |
| Vue matérialisée | 1/1 | **100% ✅** |

---

## 🎯 CONCLUSION

Le projet est **architecturalement solide** mais **fonctionnellement incomplet** pour la Phase 1 (MVP).

**Côté fort :** Platform, Identity, Academic (90% du MVP)
**Côté faible :** Finance, Grading, Attendance (20-30% du MVP)

Avec ~**3-4 semaines** de dev intensif sur les P0, ce projet peut être **production-ready**.

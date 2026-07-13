# 🔍 ANALYSE COMPLÈTE : specs + diagramme vs implémentation

_Audit refait après scan complet du code, des migrations, des tests, de `SCHOOLSAAS_SPECS.md` et du diagramme `schoolsaas_diagram_v2.jpg`._

## 📊 Résumé exécutif

- **Entités cœur présentes : 25/25** ✅
- **Entité supplémentaire : `PromotionBatch`** ✅
- **Routes HTTP réellement déclarées : 94**
- **Tests exécutés : 138** → **0 échec**, **0 erreur**, **5 skipped**
- **Couverture JaCoCo : 63.02% lignes · 61.01% instructions · 36.97% branches**
- **Score global réaliste : 7.5/10**
- **Verdict :** base backend **solide**, mais **pas encore totalement conforme** aux specs et **pas encore prête production**.

## ✅ Ce qui est bien fait

### 1. Architecture générale

- Découpage **par domaine** cohérent : `platform`, `identity`, `academic`, `enrollment`, `finance`, `grading`, `attendance`, `communication`, `dashboard`, `infrastructure`.
- Utilisation saine de Spring Boot, JPA, Flyway, Security, validation, cache, OpenAPI.
- `ApiResponse<T>` est appliqué de façon homogène et suit bien la convention API définie dans les specs.

### 2. Base multitenant

- Le projet a une **vraie base multitenant schema-per-tenant**.
- `TenantContext`, `SchemaMultiTenantConnectionProvider`, `JwtAuthenticationFilter` et `TenantMigrationService` sont en place.
- Les noms de schéma sont **validés par regex** avant concaténation SQL.
- `DashboardService` qualifie explicitement le schéma courant et protège le cache par tenant.

### 3. Onboarding plateforme

- L’onboarding est bien pensé :
  1. unicité slug/email,
  2. création `School`,
  3. création abonnement,
  4. création du schéma,
  5. exécution Flyway tenant,
  6. création du directeur.
- Les migrations tenant seedent bien les **levels** et les **templates SMS**.

### 4. Fond métier déjà crédible

- `StudentService` est propre, avec validation et normalisation du numéro guinéen.
- `EnrollmentService` gère l’unicité d’inscription annuelle et la génération de frais.
- `PaymentService` vérifie les allocations et les surpaiements.
- `ReportCardService` calcule bien la moyenne pondérée et le classement.
- `PromotionService` a déjà une base utile et un bug réel a été corrigé sur la validation d’année scolaire.

### 5. Tests

- La base de tests est maintenant **très meilleure qu’avant**.
- Les contrôleurs critiques ont des tests MVC.
- Les services centraux ont des tests unitaires ciblés.
- La couverture reste imparfaite, mais le projet n’est plus “à l’aveugle”.

## ⚠️ Vue d’ensemble : ce qui n’est pas encore conforme

Le projet est **plus avancé que la moyenne d’un MVP backend**, mais il reste plusieurs écarts importants :

1. **des endpoints manquent encore**, surtout sur les flux de consultation détaillée et certains workflows finance/SMS/promotion ;
2. **plusieurs endpoints existent mais renvoient encore du placeholder/TODO** ;
3. **certaines règles métier des specs ne sont pas complètement appliquées** ;
4. **plusieurs enums et statuts divergent des specs et du diagramme** ;
5. **des relations JPA du diagramme sont remplacées par de simples UUID**, ce qui simplifie le code mais affaiblit la richesse du modèle ;
6. **la sécurité est bonne en base**, mais pas totalement alignée aux accès exacts des specs ;
7. **la prod-readiness est incomplète** : PDF bulletin non branché, providers SMS réels absents, métriques Micrometer absentes, tests d’intégration désactivés.

---

## 1️⃣ Conformité structurelle : diagramme de classes vs code

## Entités présentes

| Domaine | Attendu specs/diagramme | Présent dans le code | État |
|---|---:|---:|---|
| platform | 4 | 4 | ✅ |
| identity | 2 | 2 | ✅ |
| academic | 6 | 6 | ✅ |
| enrollment | 2 | 2 | ✅ |
| timetable | 2 | 2 | ✅ |
| grading | 2 | 2 | ✅ |
| attendance | 1 | 1 | ✅ |
| finance | 4 | 4 | ✅ |
| communication | 2 | 2 | ✅ |
| **Total cœur** | **25** | **25** | ✅ |

### Entité supplémentaire

- `PromotionBatch` : **hors diagramme initial**, mais utile pour industrialiser F-19.  
  **Bon ajout**, à condition que le workflow final respecte les specs.

## Aucune classe cœur n’est vraiment “manquante”

Sur le plan purement structurel, le code a maintenant **toutes les entités métier principales** attendues.

## Divergences acceptables entre diagramme et code

Certaines différences avec le diagramme sont **acceptables** :

- le diagramme montre beaucoup de `schoolId` dans les tables tenant ; avec un vrai **schema-per-tenant**, ces colonnes deviennent moins nécessaires ;
- le diagramme montre souvent des `Long`, alors que le code utilise des **UUID**, ce qui est en réalité mieux aligné avec les payloads des specs ;
- le diagramme montre `User.fullName`, alors que les specs détaillées demandent `firstName` + `lastName` ; le code suit plutôt bien les specs sur ce point.

## Divergences structurelles problématiques

### Champs absents ou divergents

| Élément | Attendu | Code actuel | Impact |
|---|---|---|---|
| `Student.status` | `StudentStatus` | `isActive` seulement | Perte d’expressivité métier |
| `AcademicYear.status` | enum `YearStatus` | `String` | Moins de sûreté |
| `SchoolSubscription.status` | enum dédié | `String` | Moins de sûreté |
| `SubscriptionPayment.paymentMethod` | type métier | `String` | Divergence diagramme |
| `SmsTemplate.isActive` | présent au diagramme/spec logique | absent | Pas de désactivation fine |
| `SmsLog.templateId` | attendu au diagramme | pas de relation/template stocké | traçabilité réduite |
| `Payment.receivedBy` | attendu au diagramme | `recordedBy` UUID | acceptable mais incomplet |
| `StudentEnrollment.status` | `ENROLLED/TRANSFERRED/WITHDRAWN/GRADUATED` | `ACTIVE/INACTIVE/...` | vraie divergence métier |

### Relations manquantes ou affaiblies

Plusieurs relations du diagramme existent dans la base logique, mais **pas comme vraies relations JPA** :

- `SchoolClass.academicYear` → simple `UUID academicYearId`
- `StudentEnrollment.student` / `schoolClass` / `academicYear` → UUID au lieu de relations
- `ClassSubject.schoolClass` → UUID `classId`
- `TimetableEntry.classSubject` / `timeSlot` / `academicYear` / `term` → UUIDs
- `Payment.student` / `receivedBy` → UUIDs
- `Attendance.recordedBy` → UUID
- `SmsLog.template` / `student` → non relationnés

### Lecture

Ce n’est **pas bloquant pour faire tourner le produit**, mais :

- le mapping est plus fragile,
- les contrôleurs rechargent trop souvent “à la main”,
- certaines DTOs retournent des valeurs incomplètes ou bricolées,
- la conformité au diagramme est donc **partielle**, pas totale.

---

## 2️⃣ Enums : présents, mais plusieurs ne sont pas conformes

## Bien

- `EvaluationType` est maintenant bien aligné : `DEVOIR`, `COMPOSITION`, `ORAL`, `TP`.
- `FeeStatus` couvre l’essentiel.

## Problèmes réels

| Enum / statut | Specs / diagramme | Code actuel | État |
|---|---|---|---|
| `EnrollmentStatus` | `ENROLLED`, `TRANSFERRED`, `WITHDRAWN`, `GRADUATED` | `ACTIVE`, `INACTIVE`, `TRANSFERRED`, `GRADUATED`, `DROPPED_OUT`, `SUSPENDED` | ❌ |
| `PromotionStatus` | `PENDING`, `PROMOTED`, `REPEATED`, `GRADUATED` | `PROMOTED`, `RETAINED`, `CONDITIONAL`, `PENDING`, `OVERRIDDEN` | ❌ |
| `PaymentMethod` | `CASH`, `ORANGE_MONEY`, `MTN_MONEY`, `WAVE`, `BANK_TRANSFER`, `CHECK` | `CASH`, `BANK_TRANSFER`, `CHECK`, `CREDIT_CARD`, `MOBILE_MONEY`, `WIRE_TRANSFER`, `CRYPTO` | ❌ |
| `ReportCardStatus` | `DRAFT`, `PUBLISHED`, `SENT_TO_PARENT` | `DRAFT`, `GENERATED`, `PUBLISHED`, `ARCHIVED`, `CORRECTED` | ❌ |
| `AttendanceStatus` | `PRESENT`, `ABSENT`, `LATE`, `EXCUSED` | valeurs supplémentaires (`JUSTIFIED`, `ABSENT_UNJUSTIFIED`) | ⚠️ |
| `Period` | `FULL_DAY`, `MORNING`, `AFTERNOON` | `EVENING` en plus | ⚠️ |
| `FeeType` | `TUITION`, `REGISTRATION`, `CANTEEN`, `TRANSPORT`, `EXAM` | + `ACTIVITY`, `OTHER` | ⚠️ |
| `PaymentStatus` | spec minimale | plus riche côté code | ✅/⚠️ |

### Conclusion enums

- **Présence : bonne**
- **Conformité métier : moyenne**
- Le projet a suffisamment d’enums, mais **pas encore les bons contrats métier partout**.

---

## 3️⃣ Scorecard fonctionnelle par feature

| Feature | Score | État | Commentaire |
|---|---:|---|---|
| F-01 Onboarding | 9/10 | ✅ solide | Flux complet et cohérent |
| F-02 Auth | 7/10 | 🟡 | login/refresh bien posés, logout/public + claims ambigus |
| F-03 Utilisateurs | 8/10 | ✅ bon | CRUD + teacher auto-create bien gérés |
| F-04 Années scolaires | 7/10 | 🟡 | bon CRUD, clôture incomplète |
| F-05 Trimestres | 6/10 | 🟡 | ouverture/fermeture OK, GET/PUT incomplets |
| F-06 Niveaux/classes | 6/10 | 🟡 | base bonne, endpoints manquants et update discutable |
| F-07 Matières/affectations | 6/10 | 🟡 | affectation partielle, endpoints manquants |
| F-08 Élèves | 7/10 | 🟡 | CRUD bon, upload photo absent |
| F-09 Inscriptions | 6/10 | 🟡 | inscription utile, lecture/listing partiels |
| F-10 Structure des frais | 7/10 | 🟡 | CRUD bon, contrat API incomplet |
| F-11 Suivi frais élèves | 5/10 | 🟠 | summary placeholder, endpoints manquants |
| F-12 Paiements | 6/10 | 🟡 | create/cancel partiels, lecture incomplète |
| F-13 SMS | 5/10 | 🟠 | moteur/logging OK, use cases incomplets |
| F-14 Dashboard | 8/10 | ✅ bon | vue matérialisée + cache tenant-safe |
| F-15 Notes | 6/10 | 🟡 | base métier présente, contrôles incomplets |
| F-16 Bulletins | 5/10 | 🟠 | calcul OK, PDF/publication non finie |
| F-17 Présences | 6/10 | 🟡 | saisie OK, résumé non conforme |
| F-18 Emploi du temps | 4/10 | 🟠 | CRUD basique, lecture hebdo placeholder |
| F-19 Promotions | 4/10 | 🟠 | batch utile mais workflow specs non terminé |

## Score par axe

| Axe | Score |
|---|---:|
| Architecture | 8.5/10 |
| Modèle métier | 7.5/10 |
| Conformité specs | 6.8/10 |
| Sécurité | 6.8/10 |
| Tests | 7.9/10 |
| Production readiness | 6.3/10 |
| **Global** | **7.5/10** |

---

## 4️⃣ Ce qui manque encore par rapport aux specs

## Endpoints manquants ou non conformes

### Academic

- `GET /academic-years/{id}`
- `GET /terms/{id}`
- `PUT /terms/{id}`
- `PUT /levels/{id}`
- `GET /classes/{id}/students`
- `PUT /class-subjects/{id}`
- `GET /teachers/{teacherId}/subjects`

### Enrollment

- `POST /students/{id}/photo`
- `GET /enrollments`
- `GET /enrollments/{id}`
- `GET /students/{studentId}/enrollments`

### Finance

- `GET /academic-years/{yearId}/fee-structures` sous la forme exacte attendue
- `GET /students/{studentId}/fees`
- `POST /enrollments/{enrollmentId}/generate-fees`
- `GET /fees/unpaid`
- `GET /payments`
- `GET /payments/{id}`
- `GET /payments/student/{studentId}/pending-fees`

### Communication

- `PUT /sms/templates/{id}`
- `POST /sms/fee-reminder/{studentFeeId}`
- `POST /sms/bulk-reminders`
- `GET /sms/stats`

### Promotions

Le code expose surtout `/promotion-batches/**`, alors que les specs demandent :

- `GET /promotions?yearId=&classId=`
- `POST /promotions/{enrollmentId}/validate`
- `POST /promotions/{enrollmentId}/override`
- `POST /promotions/process-class/{classId}`

## Endpoints présents mais encore incomplets

- `GET /report-cards/{id}/pdf` → retourne encore une chaîne/placeholder
- `GET /timetable/class/{classId}` → réponse TODO
- `GET /timetable/teacher/{teacherId}` → réponse vide/TODO
- `GET /students/{studentId}/fees/summary` → placeholder

---

## 5️⃣ Ce qui n’est pas bien fait aujourd’hui

## A. Des contrôleurs contournent la couche service

C’est un des défauts majeurs actuels.

Exemples :

- `TimetableController` crée/met à jour directement `TimetableEntry` via repository au lieu de passer par une vraie logique métier de conflit.
- `GradeController` fait une partie de la logique de lecture/mise à jour côté contrôleur.
- `PaymentController` duplique des routes déjà présentes dans `FeeStructureController`.
- `ClassController.updateClass()` réutilise `createClass()` au lieu d’un vrai update métier dédié.

### Impact

- règles métier contournables,
- duplication,
- maintenance plus fragile,
- cohérence incomplète entre endpoints.

## B. Plusieurs TODO sont encore au cœur du produit

Les specs les plus visibles côté métier ont encore des TODO :

- génération et streaming PDF du bulletin,
- SMS bulletin publié,
- détermination de la classe suivante en promotion,
- création de la nouvelle inscription lors de la promotion,
- construction de la grille hebdomadaire d’emploi du temps,
- résumé complet des frais élève.

## C. Sécurité pas totalement alignée aux specs

Exemples :

- `logout` est public alors qu’il est censé être authentifié ;
- `DashboardController` autorise `TEACHER`, alors que les specs parlent de `DIRECTOR` et `ACCOUNTANT` ;
- certains endpoints “ALL_ROLES” sont codés en `isAuthenticated()`, ce qui est plus large mais moins explicite ;
- pas de contrôle fin parent/teacher sur certaines lectures “isAuthenticated”.

## D. Contrats DTO incomplets

Exemples :

- `PaymentResponse` ne contient pas encore correctement :
  - `paymentMethodLabel`,
  - `totalRemainingAfter`,
  - `newBalance` par allocation.
- `AttendanceSummaryResponse` ne suit pas le format des specs.
- `ReportCardResponse.studentName` est encore dérivé d’un UUID dans certains cas.

## E. Trop de `String` / UUID “nus”

Le code fonctionne, mais :

- plusieurs statuts restent en `String`,
- plusieurs relations du diagramme sont aplaties en UUID,
- cela réduit la sûreté de compilation et la richesse métier.

## F. Gestion d’erreur trop tolérante à quelques endroits

Points faibles :

- `SmsService` attrape tout et n’échoue jamais côté appelant ;
- `FeeReminderService` commente l’envoi réel et absorbe les erreurs ;
- `LocalStorageService.delete()` loggue seulement un warning ;
- `PdfGeneratorService` jette un `RuntimeException` générique.

---

## 6️⃣ Ce qui est manquant côté classes / composants techniques

## Manquants ou non branchés

- `src/main/resources/templates/report-card.html` ❌
- providers SMS réels type **Orange** / **Twilio** ❌
- endpoints de stats SMS et rappels unitaires ❌
- implémentation complète de publication de bulletin ❌
- métriques Micrometer métier `schoolsaas.*` ❌

## Présents mais pas encore exploités jusqu’au bout

- `PdfGeneratorService` ✅ présent mais non utilisé dans le flux bulletin
- `StorageService` / `LocalStorageService` ✅ présents mais non branchés sur les photos élèves et PDF bulletin
- `LoggingSmsProvider` ✅ présent mais seul provider réel disponible

---

## 7️⃣ Conformité aux exigences transversales

| Exigence | État | Commentaire |
|---|---|---|
| TX-01 Multitenancy | ✅ bon | vraie base schema-per-tenant |
| TX-02 Sécurité | 🟡 moyen+ | bonne base JWT/RBAC, écarts d’accès |
| TX-03 Flyway | ✅ bon | public + tenant, migrations réelles |
| TX-04 Stockage fichiers | 🟡 partiel | stockage local prêt, pas complètement branché |
| TX-05 Micrometer | ❌ absent | dépendance actuator présente, pas de métriques métier |
| TX-06 Variables d’environnement | 🟡 partiel | JWT secret imposé, config OK, mais pas tous les providers réels |

## Écart de stack

- Les specs annoncent **Java 21**.
- Le `pom.xml` est actuellement en **Java 17**.

Ce n’est pas forcément bloquant, mais c’est une **non-conformité explicite** à la stack cible.

---

## 8️⃣ État des tests et qualité réelle

## Chiffres actuels

- **138 tests exécutés**
- **0 failure**
- **0 error**
- **5 skipped**
- **63.02% line coverage**
- **36.97% branch coverage**

## Ce que ça veut dire vraiment

Le projet est maintenant **nettement mieux sécurisé par les tests** sur :

- onboarding,
- auth,
- users,
- student/enrollment,
- paiements,
- grades,
- report cards,
- promotions,
- attendance,
- timetable,
- plusieurs contrôleurs HTTP.

## Mais il reste des angles morts

- tests d’intégration multitenant désactivés,
- test OpenAPI désactivé,
- test boot global désactivé,
- test dashboard désactivé,
- peu de couverture end-to-end réelle sur PostgreSQL + Flyway + sécurité + tenant context.

### Conclusion tests

- **Bonne progression**
- **Bon niveau de confiance local**
- **Pas encore une preuve de prod-readiness**

---

## 9️⃣ Conclusion honnête

## Ce projet est bien fait sur plusieurs fondamentaux

Particulièrement :

- l’architecture par domaine,
- la base multitenant,
- l’onboarding,
- le socle sécurité/JWT,
- le dashboard tenant-safe,
- la progression des tests.

## Ce projet n’est pas encore totalement bien fait sur la finition métier

Les principaux points faibles sont :

- conformité incomplète des enums/statuts,
- contrôleurs encore trop “intelligents”,
- TODO encore visibles sur des features majeures,
- promotion et timetable pas assez finis,
- PDF bulletin non terminé,
- finance/SMS pas encore complètement fermés.

## Verdict final

- **Codebase solide : oui**
- **Conforme aux specs : partiellement**
- **Conforme au diagramme : structurellement oui, relationnellement partiellement**
- **MVP exploitable en interne : oui, avec prudence**
- **Prêt production : non, pas encore**

## Recommandations prioritaires

### P0 — à faire avant de parler de conformité forte

1. Finaliser **F-16** : vrai PDF + template + storage + SMS + `SENT_TO_PARENT`
2. Finaliser **F-18** : lecture hebdo réelle + validation de conflits utilisée partout
3. Finaliser **F-19** : vraie promotion, classe suivante, nouvelle inscription, override endpoint
4. Corriger les **enums/statuts métier** pour coller aux specs
5. Supprimer les **placeholders/TODO** exposés en API

### P1 — à faire avant prod

1. Déplacer toute la logique métier résiduelle des contrôleurs vers les services
2. Compléter les endpoints finance/SMS encore manquants
3. Reserrer les règles de sécurité exactement selon les specs
4. Ajouter les métriques Micrometer métier
5. Réactiver de vrais tests d’intégration PostgreSQL/multitenant

### Score final recommandé

**7.5/10** — très bonne base technique, mais encore **trop d’écarts métier visibles** pour afficher un 9+/10 honnête.

# SchoolSaaS — Suivi d'avancement vs. spécifications

> **Document vivant** — à remettre à jour au fil de l'avancement (relance-moi : *"mets à jour le fichier de suivi"*).
> Dernière analyse : **2026-07-16**, sur la branche `develop`, en comparant le code réel à `SCHOOLSAAS_SPECS.md` et `schoolsaas_diagram_v2.jpg`.
> Méthodologie : chaque fonctionnalité est notée sur la présence des endpoints listés dans les specs **et** sur l'implémentation réelle de la logique métier associée (pas seulement l'existence du endpoint). Un endpoint qui existe mais renvoie un stub/TODO n'est pas compté comme fait.

---

## Score global : **~70 %**

| Phase | Moyenne | Détail |
|---|---|---|
| **Phase 1 — MVP** (F-01 à F-14) | **71 %** | Le socle (onboarding, auth, structure académique, frais) est solide ; les paiements et le SMS restent en retrait. |
| **Phase 2 — Notes/bulletins/présences** (F-15 à F-17) | **78 %** | Présences très abouties ; bulletins bloqués par l'absence de PDF/SMS. |
| **Phase 3 — Emploi du temps/avancement** (F-18 à F-19) | **48 %** | La détection de conflits et le calcul de moyenne fonctionnent ; l'action finale (créer l'inscription suivante, construire la grille) n'est pas implémentée. |
| **Exigences transversales** (TX-01 à TX-06) | **68 %** | Multi-tenant et migrations excellents ; métriques Micrometer absentes, stockage fichiers non branché. |

---

## Tableau récapitulatif

| ID | Fonctionnalité | % | Statut |
|----|----------------|---|--------|
| F-01 | Onboarding école | 90 % | 🟢 |
| F-02 | Authentification | 85 % | 🟢 |
| F-03 | Gestion utilisateurs | 88 % | 🟢 |
| F-04 | Années scolaires | 85 % | 🟢 |
| F-05 | Trimestres | 60 % | 🟡 |
| F-06 | Niveaux et classes | 75 % | 🟡 |
| F-07 | Matières et affectation profs | 65 % | 🟡 |
| F-08 | Élèves (CRUD) | 80 % | 🟢 |
| F-09 | Inscriptions + génération frais | 70 % | 🟡 |
| F-10 | Structure des frais | 95 % | 🟢 |
| F-11 | Suivi frais élèves | 45 % | 🔴 |
| F-12 | Paiements | 60 % | 🟡 |
| F-13 | SMS | 40 % | 🔴 |
| F-14 | Dashboard | 90 % | 🟢 |
| F-15 | Saisie des notes | 80 % | 🟢 |
| F-16 | Bulletins PDF | 65 % | 🟡 |
| F-17 | Présences + SMS absence | 90 % | 🟢 |
| F-18 | Emploi du temps | 55 % | 🟡 |
| F-19 | Avancement élèves fin d'année | 40 % | 🔴 |
| TX-01 | Multitenancy | 95 % | 🟢 |
| TX-02 | Sécurité | 90 % | 🟢 |
| TX-03 | Migrations Flyway | 100 % | 🟢 |
| TX-04 | Stockage fichiers | 30 % | 🔴 |
| TX-05 | Métriques Micrometer | 0 % | 🔴 |
| TX-06 | Variables d'environnement | 95 % | 🟢 |

🟢 ≥ 75 % · 🟡 45–74 % · 🔴 < 45 %

---

## Détail par fonctionnalité

### F-01 · Onboarding école — 90 %
**Fait :** création School+schema+migration Flyway tenant+DIRECTOR+SchoolSubscription trial 30j, retour slug/schemaName/message (`OnboardingService.java`).
**Écart volontaire :** l'unicité d'email n'est plus vérifiée (choix architectural documenté — un propriétaire peut créer plusieurs écoles avec le même email de contact ; `schools.email` n'est plus `UNIQUE` en base).
**Évolution vs spec :** le rôle DIRECTOR n'est plus un champ `User.role` mais une ligne `user_roles` + catalogue `public.roles` — plus flexible, fonctionnellement équivalent.

### F-02 · Authentification — 85 %
**Fait :** login (`AuthController`/`AuthService`), refresh, structure de réponse conforme (accessToken/refreshToken/expiresIn/user{...}).
**Évolution vs spec :** le flux de login a été étendu à un modèle multi-écoles par personne (`Person`/`SchoolMembership`) — dépasse la spec initiale (login simple tenantSlug+email+password), reste compatible.
**Écart :** `POST /api/v1/auth/logout` est déclaré `[AUTHENTIFIÉ]` dans la spec mais `SecurityConfig` le laisse `permitAll()` comme tout `/api/v1/auth/**` — n'importe qui peut l'appeler sans token.

### F-03 · Gestion des utilisateurs — 88 % ✅ (corrigé le 2026-07-16)
**Fait :** CRUD complet (`UserController`), toutes les routes en `hasRole('DIRECTOR')` conforme à la spec, soft-delete (`isActive=false`), création automatique de `Teacher` si role=TEACHER.
**✅ Corrigé :** `UserService.createUser()` assigne désormais réellement le rôle demandé via une nouvelle méthode `assignRole()` — résolution du code de rôle en UUID (`RoleCatalogService.getIdByCode`) puis sauvegarde d'un `UserRoleAssignment`. Rôle validé contre `DIRECTOR`/`TEACHER`/`ACCOUNTANT` (`INVALID_ROLE` sinon, conforme à la spec). Couvert par un nouveau test (`UserServiceTest.createUser_WithTeacherRole_...` étendu + `createUser_WithInvalidRole_...`).
**Reste :** `updateUser()` ne permet toujours pas de changer le rôle d'un utilisateur existant (non traité dans la spec comme un besoin explicite, mais un gap mineur si le produit en a besoin plus tard).

### F-04 · Années scolaires — 85 %
**Fait :** `isCurrent` unique (reset SQL), immutabilité si `CLOSED`, `getCurrentYear()`, `closeYear()`.
**Manque :** `GET /academic-years/{id}` n'est pas exposé dans `AcademicYearController` (la méthode `getYearById` existe dans le service mais n'a pas de endpoint).
**Écart volontaire :** la clôture d'année ne déclenche plus automatiquement le service de promotion (devenu un workflow manuel par lot, voir F-19) — cohérent avec le fait que la spec elle-même reportait ce lien à la Phase 2.

### F-05 · Trimestres — 60 %
**Fait :** `isCurrent` unique par année, ouverture/fermeture de la saisie des notes.
**Manque :** `GET /terms/{id}` et `PUT /terms/{id}` n'existent pas du tout — impossible de consulter ou modifier un trimestre individuellement via l'API.

### F-06 · Niveaux et classes — 75 %
**Fait :** classes CRUD complet + filtrage par année, niveaux (liste + création).
**Manque :** `PUT /levels/{id}` (impossible de corriger un niveau), `GET /classes/{id}/students` (lister les élèves d'une classe) absent du contrôleur.

### F-07 · Matières et affectation enseignants — 65 %
**Fait :** matières CRUD, affectation classe/matière/enseignant (création + suppression).
**Manque :** `PUT /class-subjects/{id}` (pas de modification d'une affectation existante), `GET /teachers/{teacherId}/subjects` absent.

### F-08 · Élèves — 80 %
**Fait :** CRUD complet avec recherche (`?search=`), pagination, soft-delete, génération `EL-YYYY-NNNN` par trigger PostgreSQL (`fn_generate_student_number`, confirmé en migration).
**Manque :** `POST /students/{id}/photo` (upload photo) absent — `StorageService` existe (voir TX-04) mais n'est jamais appelé depuis ce contrôleur.

### F-09 · Inscriptions — 70 %
**Fait :** création d'inscription **avec génération automatique des frais** correctement câblée (`EnrollmentService.enroll()` appelle bien `StudentFeeService.generateFeesForEnrollment()`), transfert, retrait.
**Manque :** `GET /enrollments` (liste globale), `GET /enrollments/{id}` (par id), `GET /students/{studentId}/enrollments` — trois endpoints de consultation absents.

### F-10 · Structure des frais — 95 %
**Fait :** CRUD complet, filtrage par année (en query param plutôt qu'en sous-route dédiée — écart de forme mineur, sans impact fonctionnel).

### F-11 · Frais élèves et suivi — 45 % 🔴
**Fait :** application de remise (`PUT /student-fees/{id}/discount`), liste des frais par inscription.
**🔴 Stub confirmé :** `GET /students/{studentId}/fees/summary` renvoie un objet **codé en dur** (`studentName("TODO")`, tous les montants à zéro) — commentaire explicite `// TODO: Implement in Phase 5` (`FeeStructureController.java:194-202`).
**Manque :** `GET /students/{studentId}/fees` (liste simple), `POST /enrollments/{enrollmentId}/generate-fees` (régénération manuelle), `GET /fees/unpaid` — tous absents.

### F-12 · Paiements — 60 %
**Fait :** création avec vérifications `ALLOCATION_MISMATCH`, `FEE_ALREADY_PAID`, `OVER_PAYMENT` — bonne rigueur métier. Annulation dans les 24h avec note d'audit (pas de suppression).
**🔴 Bug confirmé :** la date de paiement fournie par le client est ignorée — `PaymentService.java:49` : `// .paymentDate(request.getPaymentDate().atStartOfDay()) // Simplification` est commentée. Impossible d'enregistrer un paiement à une date antérieure (ex. saisie en retard).
**Manque :** confirmation SMS au parent non envoyée (`// TODO: CommunicationService`, ligne 86) ; `GET /payments` (liste filtrable), `GET /payments/{id}`, `GET /payments/student/{studentId}/pending-fees` absents.

### F-13 · SMS — 40 % 🔴
**Fait :** `SmsService.sendTemplatedSms` bien conçu (log de toute tentative, statut PENDING/SENT/FAILED, provider abstrait). Scheduler de rappels de frais et de bascule OVERDUE tous deux fonctionnels et robustes (isolation par tenant, gestion d'échec par école — voir `SmsScheduler.java`).
**Manque :** sur 8 endpoints attendus, seuls 3 existent (`GET/POST /templates`, `GET /logs`) : `PUT /templates/{id}`, `POST /fee-reminder/{studentFeeId}` (déclenchement manuel), `POST /bulk-reminders`, `GET /stats` absents. Le provider Orange n'existe pas (seul `LoggingSmsProvider` — voir aussi `ANALYSE_PROJET.md`) : aucun SMS n'est réellement envoyé en pratique.

### F-14 · Dashboard — 90 %
**Fait :** endpoint conforme, lecture depuis `mv_dashboard_stats` (vue matérialisée confirmée en migration), cache Caffeine, scheduler de rafraîchissement (`DashboardScheduler`).

### F-15 · Saisie des notes — 80 %
**Fait :** contrôle `GRADES_ENTRY_CLOSED`, contrôle qu'un TEACHER ne note que ses propres `ClassSubject` (`GradeService.verifyTeacherAssignment`), contrainte d'unicité et plage 0-20 **imposées en base** (`uq_grade`, `chk_grade_value` dans la migration tenant).
**Écart :** le champ `enteredBy` (qui doit tracer l'utilisateur ayant saisi la note) n'est jamais renseigné — confirmé par un warning de compilation MapStruct (`Unmapped target property: "enteredBy"`). Une violation de `uq_grade` remonte en `500 INTERNAL_SERVER_ERROR` générique plutôt qu'en `409 DUPLICATE_GRADE`, faute de handler pour `DataIntegrityViolationException` dans `GlobalExceptionHandler`.

### F-16 · Bulletins scolaires — 65 %
**Fait :** génération de bulletins avec moyenne pondérée par coefficient et classement (`ReportCardService.generateForClass`/`calculateWeightedAverage`/`calculateAndUpdateRankings`) — logique fidèle à la spec et bien testée.
**🔴 Manque :** l'étape critique de `publish()` s'arrête à `status=PUBLISHED` — la génération PDF et l'envoi SMS sont des `TODO` explicites (`ReportCardService.java`, commentaires "Phase 4"). Le statut `SENT_TO_PARENT` n'est donc jamais atteint. `GET /report-cards/{id}/pdf` existe côté contrôleur mais renvoie un texte de substitution, pas un vrai PDF.

### F-17 · Présences — 90 %
**Fait :** contrainte d'unicité `ATTENDANCE_ALREADY_RECORDED`, notification SMS automatique au parent sur `ABSENT` via le template `absence_notification` — implémentation fidèle à la spec (`AttendanceService.java`).

### F-18 · Emploi du temps — 55 %
**Fait :** détection de conflits solide et conforme (`CLASS_TIMESLOT_CONFLICT`, `TEACHER_TIMESLOT_CONFLICT` dans `TimetableService.createEntry`), CRUD des créneaux horaires.
**🔴 Manque :** la construction de la grille hebdomadaire (`WeeklyTimetableResponse` avec nom de matière/enseignant/salle résolus) est un `TODO` explicite dans `TimetableController` (`.className("TODO")` codé en dur) ; la vue « emploi du temps enseignant » n'est pas non plus construite.

### F-19 · Avancement des élèves — 40 % 🔴
**Fait :** workflow par lot bien structuré (créer/valider/exécuter un `PromotionBatch`), calcul de la moyenne finale sur 3 trimestres (`PromotionService.calculateFinalAverage`).
**🔴 Manque le cœur de la fonctionnalité :** `executePromotion()` détermine correctement PROMOTED/REPEATED mais **ne crée jamais la nouvelle inscription** pour l'année suivante (`// TODO: Create new enrollment for next academic year`, `PromotionService.java:216`) — après « exécution », rien ne change concrètement pour l'élève. Le calcul de la classe suivante (`nextClassId`) et la détection GRADUATED (dernière classe d'un niveau) sont aussi des `TODO`. La méthode `overridePromotionDecision` existe dans le service mais **n'est exposée par aucun endpoint** — inatteignable depuis l'API.

---

## Exigences transversales

### TX-01 · Multitenancy — 95 %
`TenantContext`, `TenantIdentifierResolver`, `SchemaMultiTenantConnectionProvider`, validation regex du nom de schéma, nettoyage MDC dans le `finally` du filtre — tout est en place et déjà durci par plusieurs corrections de bugs de prod documentées dans le code.

### TX-02 · Sécurité — 90 %
La matrice de rôles de la spec est respectée dans les `@PreAuthorize` de tous les contrôleurs vérifiés (gestion vs consultation vs DELETE). CORS corrigé récemment pour lire la config par environnement (voir historique de commits). Écart mineur : `/auth/logout` public au lieu d'authentifié (voir F-02).

### TX-03 · Migrations Flyway — 100 %
Les 4 fonctions/triggers demandés existent tous (`fn_generate_student_number`, `fn_generate_receipt_number`, `fn_recalculate_fee_status`, `fn_update_updated_at`) ainsi que `mv_dashboard_stats`. Couverture exhaustive, confirmée directement dans `V1__init_tenant_schema.sql`.

### TX-04 · Stockage fichiers — 30 %
`StorageService`/`LocalStorageService` existent et respectent l'interface demandée, mais ne sont appelés par **aucun** contrôleur réel (ni upload photo élève F-08, ni PDF bulletin F-16, ni reçu de paiement) — infrastructure prête, jamais branchée.

### TX-05 · Métriques Micrometer — 0 %
Aucun compteur/gauge custom trouvé dans le code (`schoolsaas.payments.success`, `schoolsaas.sms.sent`, etc. absents). Seule l'auto-configuration Actuator par défaut est active.

### TX-06 · Variables d'environnement — 95 %
Toutes les variables listées sont déclarées et lues dans `application.yml`/`application-prod.yml` (JWT_SECRET obligatoire sans défaut en base, DATABASE_*, SMS_PROVIDER, STORAGE_PROVIDER, FRONTEND_URL...). `ORANGE_CLIENT_ID/SECRET` sont déclarées mais sans code consommateur (cohérent avec TX-04/F-13).

---

## 🔴 Bugs et écarts à fort impact (à traiter en priorité)

1. ~~**F-03 : aucun rôle assigné à la création d'un utilisateur via l'API**~~ — **✅ corrigé le 2026-07-16** (`UserService.assignRole()`).
2. **F-12 : date de paiement ignorée** — impossible d'enregistrer un paiement à une date différente d'aujourd'hui.
3. **F-19 : la promotion n'a aucun effet concret** — exécuter un lot de promotion ne crée pas les inscriptions de l'année suivante.
4. **F-16 : pas de PDF ni de notification SMS pour les bulletins** — la fonctionnalité s'arrête à mi-chemin.
5. **F-13 : aucun SMS n'est réellement envoyé** — provider Orange absent, seul un provider de log existe.

## Suggestions de priorité pour la suite

1. ~~Corriger l'assignation de rôle (F-03)~~ — ✅ fait.
2. Débloquer F-12 (date de paiement) — correction rapide.
3. Compléter F-19 (création d'inscription année suivante) — cœur de la Phase 3.
4. Implémenter la génération PDF (F-16) — `PdfGeneratorService` existe déjà, il ne manque que le branchement.
5. Compléter les endpoints de consultation manquants (F-05, F-09, F-11, F-12) — faible complexité, gain de couverture rapide.

---

## Journal des mises à jour

- **2026-07-16** — Analyse initiale complète (19 fonctionnalités + 6 exigences transversales), score global ~69 %.
- **2026-07-16** — F-03 corrigé (assignation de rôle à la création d'un utilisateur) : 55 % → 88 %, score global ~70 %.

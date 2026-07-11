com.schoolsaas/
│
├── SchoolSaasApplication.java
│
│   ════════════════════════════════════════════════════
│   COUCHE TRANSVERSALE — partagée par tous les domaines
│   ════════════════════════════════════════════════════
│
├── config/                              # Configuration Spring Boot
│   ├── multitenancy/
│   │   ├── TenantContext.java           # ThreadLocal du tenant courant
│   │   ├── TenantIdentifierResolver.java
│   │   └── SchemaMultiTenantConnectionProvider.java
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   └── AuthenticatedUser.java       # Principal du SecurityContext
│   ├── CacheConfig.java                 # Caffeine
│   ├── AsyncConfig.java                 # @EnableAsync, ThreadPoolExecutor
│   └── OpenApiConfig.java               # Swagger
│
├── common/                              # Code partagé entre TOUS les domaines
│   ├── entity/
│   │   └── BaseEntity.java              # id, createdAt, updatedAt en commun
│   ├── dto/
│   │   ├── ApiResponse.java             # Wrapper { success, data, error, pagination }
│   │   ├── PageRequest.java             # Paramètres pagination standardisés
│   │   └── PageResponse.java
│   ├── exception/
│   │   ├── BusinessException.java       # Exception métier typée avec HttpStatus
│   │   ├── ResourceNotFoundException.java
│   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice
│   └── util/
│       ├── PhoneUtils.java              # Validation/normalisation numéros GN
│       ├── DateUtils.java
│       └── SlugUtils.java               # Génération slugs pour tenants
│
│   ═══════════════════════════════════════════════════
│   DOMAINE PLATFORM — Schema PUBLIC (gestion SaaS)
│   Toutes ces classes lisent/écrivent dans le schema "public"
│   PAS dans le schema du tenant
│   ═══════════════════════════════════════════════════
│
├── platform/
│   ├── entity/
│   │   ├── School.java                  # L'école = le tenant
│   │   ├── SubscriptionPlan.java        # Plans tarifaires
│   │   ├── SchoolSubscription.java      # Abonnement actif d'une école
│   │   └── SubscriptionPayment.java     # Paiements de l'abonnement SaaS
│   ├── repository/
│   │   ├── SchoolRepository.java
│   │   ├── SubscriptionPlanRepository.java
│   │   └── SchoolSubscriptionRepository.java
│   ├── service/
│   │   ├── OnboardingService.java       # Inscription nouvelle école (crée le schema)
│   │   ├── SubscriptionService.java     # Gestion des abonnements
│   │   └── TenantMigrationService.java  # Flyway par tenant
│   ├── controller/
│   │   ├── PlatformController.java      # Routes /api/v1/platform/** (super-admin)
│   │   └── OnboardingController.java    # Route publique d'inscription école
│   ├── dto/
│   │   ├── request/
│   │   │   └── OnboardingRequest.java
│   │   └── response/
│   │       └── OnboardingResponse.java
│   └── scheduler/
│       └── SubscriptionScheduler.java   # Vérification expiration abonnements
│
│   ═══════════════════════════════════════════════════
│   DOMAINES MÉTIER — Schema TENANT (par école)
│   Tous ces domaines opèrent dans le schema du tenant courant
│   ═══════════════════════════════════════════════════
│
├── identity/                            # Qui peut se connecter et faire quoi
│   │   ENTITÉS: User, Teacher
│   │   RESPONSABILITÉ: authentification, autorisations, profils
│   │
│   ├── entity/
│   │   ├── User.java
│   │   └── Teacher.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── TeacherRepository.java
│   ├── service/
│   │   ├── AuthService.java             # Login, refresh token, logout
│   │   └── UserService.java             # CRUD utilisateurs
│   ├── controller/
│   │   ├── AuthController.java          # POST /api/v1/auth/**
│   │   └── UserController.java          # GET/POST /api/v1/school/users/**
│   └── dto/
│       ├── request/
│       │   ├── LoginRequest.java
│       │   └── CreateUserRequest.java
│       └── response/
│           ├── AuthResponse.java
│           └── UserResponse.java
│
├── academic/                            # La structure pédagogique de l'école
│   │   ENTITÉS: AcademicYear, Term, Level, Class, Subject, ClassSubject
│   │   RESPONSABILITÉ: organiser "qui enseigne quoi à qui et quand"
│   │   PROPRIÉTAIRE: le directeur configure tout ça en début d'année
│   │
│   ├── entity/
│   │   ├── AcademicYear.java
│   │   ├── Term.java
│   │   ├── Level.java
│   │   ├── SchoolClass.java             # "Class" est un mot réservé Java → SchoolClass
│   │   ├── Subject.java
│   │   └── ClassSubject.java
│   ├── repository/
│   │   ├── AcademicYearRepository.java
│   │   ├── TermRepository.java
│   │   ├── LevelRepository.java
│   │   ├── SchoolClassRepository.java
│   │   ├── SubjectRepository.java
│   │   └── ClassSubjectRepository.java
│   ├── service/
│   │   ├── AcademicYearService.java     # Créer/clôturer une année scolaire
│   │   ├── ClassService.java            # CRUD classes + affectation profs
│   │   └── SubjectService.java          # CRUD matières
│   ├── controller/
│   │   ├── AcademicYearController.java
│   │   ├── ClassController.java
│   │   └── SubjectController.java
│   └── dto/
│       ├── request/
│       └── response/
│
├── enrollment/                          # Les élèves et leurs inscriptions
│   │   ENTITÉS: Student, StudentEnrollment
│   │   RESPONSABILITÉ: "qui est inscrit dans quelle classe cette année"
│   │   POINT D'ENTRÉE pour les autres domaines (via studentId/enrollmentId)
│   │
│   ├── entity/
│   │   ├── Student.java
│   │   └── StudentEnrollment.java
│   ├── repository/
│   │   ├── StudentRepository.java
│   │   └── StudentEnrollmentRepository.java
│   ├── service/
│   │   ├── StudentService.java          # CRUD élèves
│   │   ├── EnrollmentService.java       # Inscriptions annuelles
│   │   └── PromotionService.java        # Avancement automatique fin d'année
│   ├── controller/
│   │   ├── StudentController.java
│   │   └── EnrollmentController.java
│   └── dto/
│       ├── request/
│       │   ├── CreateStudentRequest.java
│       │   └── EnrollStudentRequest.java
│       └── response/
│           ├── StudentResponse.java
│           ├── StudentDetailResponse.java
│           └── EnrollmentResponse.java
│
├── timetable/                           # L'emploi du temps
│   │   ENTITÉS: TimeSlot, TimetableEntry
│   │   RESPONSABILITÉ: "quelle matière, dans quelle salle, à quelle heure"
│   │   DÉPEND DE: academic (ClassSubject)
│   │
│   ├── entity/
│   │   ├── TimeSlot.java
│   │   └── TimetableEntry.java
│   ├── repository/
│   │   ├── TimeSlotRepository.java
│   │   └── TimetableEntryRepository.java
│   ├── service/
│   │   ├── TimeSlotService.java         # Gérer les créneaux horaires
│   │   └── TimetableService.java        # Construire/lire l'emploi du temps
│   ├── controller/
│   │   └── TimetableController.java
│   └── dto/
│       ├── request/
│       │   └── TimetableEntryRequest.java
│       └── response/
│           ├── TimetableEntryResponse.java
│           └── WeeklyTimetableResponse.java  # Vue grille hebdomadaire
│
├── grading/                             # Notes et bulletins
│   │   ENTITÉS: Grade, ReportCard
│   │   RESPONSABILITÉ: saisir les notes, calculer les moyennes, publier les bulletins
│   │   DÉPEND DE: academic (ClassSubject, Term), enrollment (StudentEnrollment)
│   │   APPELLE: communication (SMS bulletin disponible)
│   │
│   ├── entity/
│   │   ├── Grade.java
│   │   └── ReportCard.java
│   ├── repository/
│   │   ├── GradeRepository.java
│   │   └── ReportCardRepository.java
│   ├── service/
│   │   ├── GradeService.java            # Saisie notes, calcul moyennes pondérées
│   │   ├── ReportCardService.java       # Publication bulletins, génération PDF
│   │   └── RankingService.java          # Calcul classements par classe
│   ├── controller/
│   │   ├── GradeController.java
│   │   └── ReportCardController.java
│   └── dto/
│       ├── request/
│       │   ├── GradeEntryRequest.java
│       │   └── BulkGradeEntryRequest.java
│       └── response/
│           ├── GradeEntryResponse.java
│           ├── StudentTermReportResponse.java   # Fiche de notes d'un élève
│           ├── ClassGradeSheetResponse.java     # Tableau notes toute la classe
│           ├── ReportCardResponse.java
│           └── PromotionProposalResponse.java   # Propositions de passage
│
├── attendance/                          # Présences et absences
│   │   ENTITÉS: Attendance
│   │   RESPONSABILITÉ: appel, suivi des absences, alertes aux parents
│   │   APPELLE: communication (SMS absence)
│   │
│   ├── entity/
│   │   └── Attendance.java
│   ├── repository/
│   │   └── AttendanceRepository.java
│   ├── service/
│   │   └── AttendanceService.java       # Appel, stats absences, alertes
│   ├── controller/
│   │   └── AttendanceController.java
│   └── dto/
│       ├── request/
│       │   └── AttendanceRequest.java
│       └── response/
│           ├── AttendanceResponse.java
│           └── AttendanceSummaryResponse.java
│
├── finance/                             # Frais et paiements
│   │   ENTITÉS: FeeStructure, StudentFee, Payment, PaymentAllocation
│   │   RESPONSABILITÉ: définir les frais, suivre les paiements, générer les reçus
│   │   APPELLE: communication (SMS confirmation paiement / rappel)
│   │
│   ├── entity/
│   │   ├── FeeStructure.java
│   │   ├── StudentFee.java
│   │   ├── Payment.java
│   │   └── PaymentAllocation.java
│   ├── repository/
│   │   ├── FeeStructureRepository.java
│   │   ├── StudentFeeRepository.java
│   │   ├── PaymentRepository.java
│   │   └── PaymentAllocationRepository.java
│   ├── service/
│   │   ├── FeeStructureService.java     # CRUD grille tarifaire
│   │   ├── StudentFeeService.java       # Génération frais par élève
│   │   └── PaymentService.java          # Enregistrement paiements, reçus
│   ├── controller/
│   │   ├── FeeStructureController.java
│   │   └── PaymentController.java
│   └── dto/
│       ├── request/
│       │   ├── CreateFeeStructureRequest.java
│       │   └── CreatePaymentRequest.java
│       └── response/
│           ├── StudentFeeResponse.java
│           ├── PaymentResponse.java
│           ├── PaymentSummaryResponse.java
│           └── FeeCollectionStatsResponse.java
│
├── communication/                       # SMS et notifications
│   │   ENTITÉS: SmsTemplate, SmsLog
│   │   RESPONSABILITÉ: envoyer, logger, planifier les SMS
│   │   APPELÉ PAR: finance, grading, attendance
│   │
│   ├── entity/
│   │   ├── SmsTemplate.java
│   │   └── SmsLog.java
│   ├── repository/
│   │   ├── SmsTemplateRepository.java
│   │   └── SmsLogRepository.java
│   ├── service/
│   │   ├── SmsService.java              # Service principal d'envoi
│   │   └── SmsTemplateEngine.java       # Résolution des variables {{...}}
│   ├── scheduler/
│   │   └── SmsScheduler.java            # Rappels automatiques paiements
│   ├── controller/
│   │   └── SmsController.java
│   └── dto/
│       ├── request/
│       │   └── SendSmsRequest.java
│       └── response/
│           ├── SmsResultResponse.java
│           └── SmsStatsResponse.java
│
├── dashboard/                           # Agrégation cross-domaines
│   │   PAS D'ENTITÉS propres — lit depuis tous les domaines
│   │   RESPONSABILITÉ: stats globales, KPIs, alertes pour le directeur
│   │   DÉPEND DE: tous les domaines (via leurs services ou JdbcTemplate)
│   │
│   ├── service/
│   │   └── DashboardService.java        # Lit mv_dashboard_stats + requêtes ad-hoc
│   ├── controller/
│   │   └── DashboardController.java
│   └── dto/
│       └── response/
│           ├── DashboardStatsResponse.java
│           └── ClassroomPerformanceResponse.java
│
└── infrastructure/                      # Intégrations techniques externes
    │   Tout ce qui touche au monde extérieur (API tierces, fichiers, PDF)
    │   PAS de logique métier ici — uniquement du "comment" pas du "quoi"
    │
    ├── sms/
    │   ├── SmsProvider.java             # Interface commune
    │   ├── SmsResult.java
    │   ├── OrangeSmsProvider.java       # Implémentation Orange Guinea
    │   ├── TwilioSmsProvider.java       # Fallback international
    │   ├── LoggingSmsProvider.java      # Dev/test
    │   └── SmsProviderConfig.java       # Sélection du provider actif
    ├── storage/
    │   ├── StorageService.java          # Interface
    │   ├── LocalStorageService.java     # Dev local
    │   └── S3StorageService.java        # Production
    └── pdf/
        ├── PdfGeneratorService.java     # HTML → PDF via OpenHTMLtoPDF
        └── templates/                   # Templates Thymeleaf (copie logique)
            ├── ReportCardTemplate.java  # Données pour le bulletin
            └── ReceiptTemplate.java     # Données pour le reçu


═══════════════════════════════════════════════════════════════════════
RÈGLES D'ARCHITECTURE — À respecter absolument
═══════════════════════════════════════════════════════════════════════

1. DÉPENDANCES AUTORISÉES entre domaines
   ─────────────────────────────────────
   Un domaine peut APPELER le service d'un autre domaine.
   Un domaine ne doit JAMAIS importer l'entité d'un autre domaine.

   ✅ grading → enrollmentService.getEnrollmentById(id)
   ❌ grading → import com.schoolsaas.enrollment.entity.Student

2. COMMUNICATION entre domaines
   ────────────────────────────
   Les domaines communiquent via des IDs (Long/UUID), pas des objets entiers.
   Si un domaine a besoin d'infos d'un autre, il appelle son service
   et reçoit un DTO de réponse (jamais une entité JPA directe).

3. FLUX DE DÉPENDANCE (du plus stable au moins stable)
   ─────────────────────────────────────────────────────
   infrastructure  ←  common  ←  platform
                              ←  identity
                              ←  academic
                              ←  enrollment  ←  timetable
                                             ←  grading     ←  dashboard
                                             ←  attendance  ←  dashboard
                                             ←  finance     ←  dashboard
                              ←  communication (appelé par grading, finance, attendance)

4. OÙ METTRE UN SERVICE QUI COUPE PLUSIEURS DOMAINES ?
   ─────────────────────────────────────────────────────
   Ex: "PromotionService" utilise grading ET enrollment.
   Règle: il va dans le domaine qui EN EST LE PROPRIÉTAIRE MÉTIER.
   Le passage des élèves est une responsabilité de enrollment → PromotionService va dans enrollment/.
   Il appelle gradingService pour obtenir les moyennes.

5. DTO vs ENTITÉ
   ──────────────
   Les controllers reçoivent des Request DTOs et retournent des Response DTOs.
   Les entités JPA ne sortent JAMAIS des couches service/repository.
   Utiliser MapStruct pour les conversions entité ↔ DTO.

6. NOMMAGE COHÉRENT
   ──────────────────
   Entités:     Student, Grade, Payment           (nom métier simple)
   Repository:  StudentRepository                  (nom + Repository)
   Service:     StudentService                     (nom + Service)
   Controller:  StudentController                  (nom + Controller)
   Request DTO: CreateStudentRequest                (verbe + nom + Request)
   Response DTO:StudentResponse, StudentDetailResponse (nom + [Detail] + Response)

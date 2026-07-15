/* tslint:disable */
/* eslint-disable */

export interface AllocationRequest {
    amount: number;
    studentFeeId: string;
}

export interface AllocationResponse {
    amountAllocated: number;
    feeLabel: string;
    studentFeeId: string;
}

export interface ApiResponse<T> {
    data: T;
    error: string;
    message: string;
    pagination: PageMeta;
    success: boolean;
}

export interface AttendanceRecord {
    enrollmentId: string;
    justification: string;
    status: string;
}

export interface AttendanceRequest {
    date: Date;
    enrollmentId: string;
    justification: string;
    period: string;
    status: string;
}

export interface AttendanceResponse {
    createdAt: Date;
    date: Date;
    enrollmentId: string;
    id: string;
    justification: string;
    period: string;
    recordedBy: string;
    status: string;
    updatedAt: Date;
}

export interface AttendanceSummaryResponse {
    absentDays: number;
    attendanceRate: number;
    excusedDays: number;
    lateDays: number;
    presentDays: number;
    studentId: string;
    totalDays: number;
}

export interface AuthResponse {
    accessToken: string;
    expiresIn: number;
    redirectedToFallbackSchool: boolean;
    refreshToken: string;
    user: UserData;
}

export interface BulkAttendanceRequest {
    classId: string;
    date: Date;
    period: string;
    records: AttendanceRecord[];
}

export interface BulkGradeItem {
    comment: string;
    enrollmentId: string;
    value: number;
}

export interface BulkGradeRequest {
    classSubjectId: string;
    evaluationDate: Date;
    evaluationLabel: string;
    evaluationType: string;
    grades: BulkGradeItem[];
    termId: string;
}

export interface CreatePaymentRequest {
    allocations: AllocationRequest[];
    amount: number;
    notes: string;
    paymentDate: Date;
    paymentMethod: string;
    referenceNumber: string;
    studentId: string;
}

export interface CreatePromotionBatchRequest {
    academicYearId: string;
    classId: string;
    nextAcademicYearId: string;
    notes: string;
}

export interface CreateStudentRequest {
    address: string;
    birthCity: string;
    birthCountry: string;
    dateOfBirth: Date;
    firstName: string;
    gender: Gender;
    lastName: string;
    medicalNotes: string;
    parentName: string;
    parentPhone: string;
}

export interface CreateUserRequest {
    avatarUrl: string;
    bio: string;
    email: string;
    employeeNumber: string;
    firstName: string;
    hireDate: Date;
    lastName: string;
    password: string;
    phone: string;
    qualification: string;
    role: string;
    specialty: string;
}

export interface DashboardStatsResponse {
    activeStudents: number;
    collectionRatePct: number;
    femaleStudents: number;
    maleStudents: number;
    pendingGradeEntries: number;
    smsThisMonth: number;
    smsToday: number;
    studentsOverdue: number;
    studentsWithDebt: number;
    totalFeesCollected: number;
    totalFeesExpected: number;
    totalStudents: number;
}

export interface EnrollStudentRequest {
    academicYearId: string;
    classId: string;
    enrollmentDate: Date;
    isRepeating: boolean;
    studentId: string;
}

export interface EnrollmentResponse {
    academicYearId: string;
    academicYearLabel: string;
    classId: string;
    className: string;
    enrollmentDate: Date;
    finalAverage: number;
    id: string;
    isRepeating: boolean;
    promotionStatus: string;
    status: string;
    studentId: string;
    studentName: string;
    studentNumber: string;
}

export interface FeeStructureRequest {
    academicYearId: string;
    amount: number;
    classId: string;
    dueDate: Date;
    feeType: FeeType;
    installmentsAllowed: boolean;
    label: string;
    maxInstallments: number;
}

export interface FeeStructureResponse {
    academicYearId: string;
    amount: number;
    classId: string;
    createdAt: Date;
    dueDate: Date;
    feeType: FeeType;
    id: string;
    installmentsAllowed: boolean;
    label: string;
    maxInstallments: number;
    updatedAt: Date;
}

export interface GenerateReportCardsRequest {
    classId: string;
    termId: string;
}

export interface GradeRequest {
    classSubjectId: string;
    comment: string;
    enrollmentId: string;
    evaluationDate: Date;
    evaluationLabel: string;
    evaluationType: string;
    termId: string;
    value: number;
}

export interface GradeResponse {
    classSubjectId: string;
    comment: string;
    createdAt: Date;
    enrollmentId: string;
    evaluationDate: Date;
    evaluationLabel: string;
    evaluationType: string;
    id: string;
    termId: string;
    updatedAt: Date;
    value: number;
}

export interface LoginRequest {
    email: string;
    password: string;
    tenantSlug: string;
}

export interface OnboardingRequest {
    city: string;
    countryCode: string;
    currency: string;
    directorFirstName: string;
    directorLastName: string;
    directorPassword: string;
    email: string;
    phone: string;
    planCode: string;
    schoolName: string;
    slug: string;
    timezone: string;
}

export interface OnboardingResponse {
    message: string;
    schemaName: string;
    schoolName: string;
    tenantSlug: string;
}

export interface PageMeta {
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
}

export interface PaymentResponse {
    allocations: AllocationResponse[];
    amount: number;
    id: string;
    paymentDate: Date;
    paymentMethod: string;
    receiptNumber: string;
    referenceNumber: string;
    studentId: string;
    studentName: string;
}

export interface PromotionBatchResponse {
    academicYearId: string;
    classId: string;
    createdAt: Date;
    directorComment: string;
    executedAt: Date;
    graduatedCount: number;
    id: string;
    nextAcademicYearId: string;
    notes: string;
    promotedCount: number;
    repeatedCount: number;
    status: PromotionBatchStatus;
    totalProcessed: number;
    validationErrors: string;
}

export interface ReportCardCommentsRequest {
    directorComment: string;
    teacherComment: string;
}

export interface ReportCardResponse {
    classSize: number;
    createdAt: Date;
    directorComment: string;
    enrollmentId: string;
    generalAverage: number;
    id: string;
    pdfUrl: string;
    publishedAt: Date;
    rankInClass: number;
    status: ReportCardStatus;
    studentName: string;
    teacherComment: string;
    termId: string;
    termName: string;
}

export interface SchoolSummaryResponse {
    name: string;
    rolesSnapshot: string;
    slug: string;
}

export interface StudentFeeDiscountRequest {
    discountAmount: number;
    discountReason: string;
}

export interface StudentFeeResponse {
    amountDue: number;
    amountPaid: number;
    createdAt: Date;
    discountAmount: number;
    discountReason: string;
    dueDate: Date;
    enrollmentId: string;
    feeLabel: string;
    feeStructureId: string;
    id: string;
    status: FeeStatus;
}

export interface StudentFeeSummaryResponse {
    amountRemaining: number;
    enrollmentId: string;
    overallStatus: string;
    paidFeeCount: number;
    studentName: string;
    totalDiscount: number;
    totalDue: number;
    totalPaid: number;
    unpaidFeeCount: number;
}

export interface StudentResponse {
    dateOfBirth: Date;
    firstName: string;
    gender: Gender;
    id: string;
    isActive: boolean;
    lastName: string;
    parentName: string;
    parentPhone: string;
    photoUrl: string;
    studentNumber: string;
}

export interface SwitchSchoolRequest {
    schoolSlug: string;
}

export interface TimeSlotRequest {
    dayOfWeek: DayOfWeek;
    endTime: Date;
    label: string;
    orderIndex: number;
    startTime: Date;
}

export interface TimetableEntryDto {
    dayOfWeek: string;
    endTime: Date;
    isActive: boolean;
    roomNumber: string;
    startTime: Date;
    subjectName: string;
    teacherName: string;
}

export interface TimetableEntryRequest {
    academicYearId: string;
    classSubjectId: string;
    isActive: boolean;
    roomNumber: string;
    termId: string;
    timeSlotId: string;
}

export interface UserData {
    email: string;
    fullName: string;
    id: string;
    roles: string[];
    schoolName: string;
    schoolSlug: string;
    tenantId: string;
}

export interface UserResponse {
    avatarUrl: string;
    createdAt: Date;
    email: string;
    firstName: string;
    id: string;
    isActive: boolean;
    lastLoginAt: Date;
    lastName: string;
    phone: string;
    role: string;
}

export interface WeeklyTimetableResponse {
    classId: string;
    className: string;
    entries: TimetableEntryDto[];
}

export const enum AttendanceStatus {
    PRESENT = "PRESENT",
    ABSENT = "ABSENT",
    LATE = "LATE",
    EXCUSED = "EXCUSED",
}

export const enum BillingCycle {
    MONTHLY = "MONTHLY",
    YEARLY = "YEARLY",
}

export const enum DayOfWeek {
    MONDAY = "MONDAY",
    TUESDAY = "TUESDAY",
    WEDNESDAY = "WEDNESDAY",
    THURSDAY = "THURSDAY",
    FRIDAY = "FRIDAY",
    SATURDAY = "SATURDAY",
}

export const enum EnrollmentStatus {
    ENROLLED = "ENROLLED",
    TRANSFERRED = "TRANSFERRED",
    WITHDRAWN = "WITHDRAWN",
    GRADUATED = "GRADUATED",
}

export const enum EvaluationType {
    DEVOIR = "DEVOIR",
    COMPOSITION = "COMPOSITION",
    ORAL = "ORAL",
    TP = "TP",
}

export const enum FeeStatus {
    UNPAID = "UNPAID",
    PARTIAL = "PARTIAL",
    PAID = "PAID",
    OVERDUE = "OVERDUE",
    WAIVED = "WAIVED",
}

export const enum FeeType {
    TUITION = "TUITION",
    REGISTRATION = "REGISTRATION",
    CANTEEN = "CANTEEN",
    TRANSPORT = "TRANSPORT",
    EXAM = "EXAM",
    ACTIVITY = "ACTIVITY",
    OTHER = "OTHER",
}

export const enum Gender {
    MALE = "MALE",
    FEMALE = "FEMALE",
}

export const enum PaymentMethod {
    CASH = "CASH",
    ORANGE_MONEY = "ORANGE_MONEY",
    MTN_MONEY = "MTN_MONEY",
    WAVE = "WAVE",
    BANK_TRANSFER = "BANK_TRANSFER",
    CHECK = "CHECK",
}

export const enum PaymentStatus {
    CONFIRMED = "CONFIRMED",
    CANCELLED = "CANCELLED",
    REFUNDED = "REFUNDED",
}

export const enum Period {
    FULL_DAY = "FULL_DAY",
    MORNING = "MORNING",
    AFTERNOON = "AFTERNOON",
    EVENING = "EVENING",
}

export const enum PromotionBatchStatus {
    CREATED = "CREATED",
    VALIDATED = "VALIDATED",
    EXECUTED = "EXECUTED",
    CANCELLED = "CANCELLED",
}

export const enum PromotionStatus {
    PENDING = "PENDING",
    PROMOTED = "PROMOTED",
    REPEATED = "REPEATED",
    GRADUATED = "GRADUATED",
}

export const enum ReportCardStatus {
    DRAFT = "DRAFT",
    PUBLISHED = "PUBLISHED",
    SENT_TO_PARENT = "SENT_TO_PARENT",
}

export const enum SchoolStatus {
    TRIAL = "TRIAL",
    ACTIVE = "ACTIVE",
    SUSPENDED = "SUSPENDED",
    DELETED = "DELETED",
}

export const enum SmsCategory {
    FINANCIAL = "FINANCIAL",
    ACADEMIC = "ACADEMIC",
    ADMINISTRATIVE = "ADMINISTRATIVE",
    CUSTOM = "CUSTOM",
}

export const enum SmsStatus {
    PENDING = "PENDING",
    SENT = "SENT",
    DELIVERED = "DELIVERED",
    FAILED = "FAILED",
}

export const enum SubscriptionPaymentMethod {
    BANK_TRANSFER = "BANK_TRANSFER",
    ORANGE_MONEY = "ORANGE_MONEY",
    MTN_MONEY = "MTN_MONEY",
    WAVE = "WAVE",
    CARD = "CARD",
    CHECK = "CHECK",
}

export const enum SubscriptionPaymentStatus {
    PENDING = "PENDING",
    COMPLETED = "COMPLETED",
    FAILED = "FAILED",
    REFUNDED = "REFUNDED",
}

export const enum SubscriptionStatus {
    ACTIVE = "ACTIVE",
    EXPIRED = "EXPIRED",
    CANCELLED = "CANCELLED",
}

export const enum YearStatus {
    ACTIVE = "ACTIVE",
    CLOSED = "CLOSED",
}

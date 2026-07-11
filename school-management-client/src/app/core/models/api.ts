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

export interface AttendanceRequest {
    date: Date;
    enrollmentId: string;
    justification: string;
    period: string;
    status: string;
}

export interface AttendanceResponse {
    date: Date;
    enrollmentId: string;
    id: string;
    justification: string;
    period: string;
    status: string;
}

export interface AuthResponse {
    accessToken: string;
    expiresIn: number;
    refreshToken: string;
    user: UserData;
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

export interface CreateStudentRequest {
    address: string;
    birthCity: string;
    birthCountry: string;
    dateOfBirth: Date;
    firstName: string;
    gender: string;
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

export interface StudentResponse {
    dateOfBirth: Date;
    firstName: string;
    gender: string;
    id: string;
    isActive: boolean;
    lastName: string;
    parentName: string;
    parentPhone: string;
    photoUrl: string;
    studentNumber: string;
}

export interface UserData {
    email: string;
    fullName: string;
    id: string;
    roles: string[];
    schoolName: string;
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

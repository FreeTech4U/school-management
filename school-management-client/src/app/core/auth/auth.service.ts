import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, shareReplay, tap, throwError } from 'rxjs';

import { LoginCredentials } from '../../features/auth/models/login-credentials.model';
import { ApiResponse, AuthResponse, LoginRequest, OnboardingRequest, OnboardingResponse, UserData } from '../models';
import { MockAuthRole, MockAuthUser } from './models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authBaseUrl = 'http://localhost:8080/api/v1/auth';
  private readonly platformBaseUrl = 'http://localhost:8080/api/v1/platform';
  private readonly accessTokenKey = 'auth.accessToken';
  private readonly refreshTokenKey = 'auth.refreshToken';
  private readonly expiresAtKey = 'auth.expiresAt';
  private readonly userKey = 'auth.user';
  private readonly tenantSlugKey = 'auth.tenantSlug';
  private refreshTokenRequest$: Observable<string> | null = null;

  readonly currentUser = signal<MockAuthUser | null>(null);

  constructor(private readonly http: HttpClient) {
    this.currentUser.set(this.restoreUser());
  }

  login(credentials: LoginCredentials): Observable<AuthResponse> {
    const request: LoginRequest = {
      tenantSlug: this.getTenantSlug(),
      email: credentials.identifier.trim(),
      password: String(credentials.password ?? '')
    };

    return this.loginWithRequest(request);
  }

  loginWithRequest(request: LoginRequest): Observable<AuthResponse> {
    this.setTenantSlug(request.tenantSlug);

    return this.http.post<ApiResponse<AuthResponse>>(`${this.authBaseUrl}/login`, request).pipe(
      map((response) => response.data),
      tap((session) => this.persistSession(session))
    );
  }

  register(request: OnboardingRequest): Observable<OnboardingResponse> {
    return this.http.post<ApiResponse<OnboardingResponse>>(`${this.platformBaseUrl}/onboard`, request).pipe(
      map((response) => response.data)
    );
  }

  requestLogout(): Observable<void> {
    const hasAccessToken = Boolean(this.getAccessToken());

    if (!hasAccessToken) {
      this.clearSession();
      return of(void 0);
    }

    return this.http.post<ApiResponse<unknown>>(`${this.authBaseUrl}/logout`, {}).pipe(
      map(() => void 0),
      finalize(() => this.clearSession())
    );
  }

  refreshAccessToken(): Observable<string> {
    const refreshToken = this.getRefreshToken();

    if (!refreshToken) {
      return throwError(() => new Error('AUTH_REFRESH_TOKEN_MISSING'));
    }

    if (this.refreshTokenRequest$) {
      return this.refreshTokenRequest$;
    }

    const payload: Record<string, string> = { refreshToken };

    this.refreshTokenRequest$ = this.http
      .post<ApiResponse<AuthResponse>>(`${this.authBaseUrl}/refresh`, payload)
      .pipe(
        map((response) => response.data),
        tap((session) => this.persistSession(session)),
        map((session) => session.accessToken),
        finalize(() => {
          this.refreshTokenRequest$ = null;
        }),
        shareReplay(1)
      );

    return this.refreshTokenRequest$;
  }

  isAuthenticated(): boolean {
    return Boolean(this.getAccessToken()) && !this.isTokenExpired() && Boolean(this.currentUser());
  }

  loadSession(): Observable<UserData | null> {
    const userData = this.restoreUserData();

    if (!userData || this.isTokenExpired()) {
      this.clearSession();
      return of(null);
    }

    this.currentUser.set(this.toAuthUser(userData));
    return of(userData);
  }

  getCurrentRole(): MockAuthRole | null {
    return this.currentUser()?.role ?? null;
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem(this.accessTokenKey);
  }

  clearSession(): void {
    sessionStorage.removeItem(this.accessTokenKey);
    sessionStorage.removeItem(this.refreshTokenKey);
    sessionStorage.removeItem(this.expiresAtKey);
    sessionStorage.removeItem(this.userKey);
    this.currentUser.set(null);
  }

  private restoreUser(): MockAuthUser | null {
    const userData = this.restoreUserData();

    if (!userData || this.isTokenExpired()) {
      return null;
    }

    return this.toAuthUser(userData);
  }

  private restoreUserData(): UserData | null {
    const serializedUser = sessionStorage.getItem(this.userKey);
    if (!serializedUser) {
      return null;
    }

    try {
      return JSON.parse(serializedUser) as UserData;
    } catch {
      return null;
    }
  }

  private persistSession(session: AuthResponse): void {
    sessionStorage.setItem(this.accessTokenKey, session.accessToken);
    sessionStorage.setItem(this.refreshTokenKey, session.refreshToken);
    sessionStorage.setItem(this.expiresAtKey, String(Date.now() + session.expiresIn * 1000));
    sessionStorage.setItem(this.userKey, JSON.stringify(session.user));
    this.currentUser.set(this.toAuthUser(session.user));
  }

  private getRefreshToken(): string | null {
    return sessionStorage.getItem(this.refreshTokenKey);
  }

  private isTokenExpired(): boolean {
    const rawExpiresAt = sessionStorage.getItem(this.expiresAtKey);
    if (!rawExpiresAt) {
      return true;
    }

    const expiresAt = Number(rawExpiresAt);
    return Number.isNaN(expiresAt) || Date.now() >= expiresAt;
  }

  private toAuthUser(user: UserData): MockAuthUser {
    return {
      id: user.id,
      fullName: user.fullName,
      identifier: user.email,
      role: this.resolveRole(user.roles)
    };
  }

  private resolveRole(roles: string[]): MockAuthRole {
    const normalized = roles.map((r) => r.toLowerCase());

    if (normalized.includes('director')) return 'director';
    if (normalized.includes('accountant')) return 'accountant';

    return 'administrator';
  }

  private getTenantSlug(): string {
    return sessionStorage.getItem(this.tenantSlugKey) ?? '';
  }

  private setTenantSlug(slug: string): void {
    sessionStorage.setItem(this.tenantSlugKey, slug.trim());
  }
}

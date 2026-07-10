import { Injectable, signal } from '@angular/core';
import { Observable, delay, map, of } from 'rxjs';

import { LoginCredentials } from '../../features/auth/models/login-credentials.model';
import { MockAuthSession, MockAuthUser, MockAuthRole, MockTokenPayload } from './models/auth.models';

interface DemoAccount {
  identifier: string;
  fullName: string;
  role: MockAuthRole;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'auth.token';
  private readonly demoPassword = 'School@123';
  private readonly demoAccounts: DemoAccount[] = [
    { identifier: 'admin@school.com', fullName: 'Administrateur Demo', role: 'administrator' },
    { identifier: 'compta@school.com', fullName: 'Comptable Demo', role: 'accountant' },
    { identifier: '+2250102030405', fullName: 'Direction Demo', role: 'director' }
  ];

  readonly currentUser = signal<MockAuthUser | null>(this.restoreUser());

  login(credentials: LoginCredentials): Observable<MockAuthSession> {
    const normalizedIdentifier = this.normalizeIdentifier(credentials.identifier);

    return of({
      identifier: normalizedIdentifier,
      password: String(credentials.password ?? '')
    }).pipe(
      delay(500),
      map(({ identifier, password }) => {
        const matchedAccount = this.demoAccounts.find(
          (account) => this.normalizeIdentifier(account.identifier) === identifier
        );

        if (!matchedAccount || password !== this.demoPassword) {
          throw new Error('INVALID_CREDENTIALS');
        }

        const user: MockAuthUser = {
          id: matchedAccount.identifier,
          fullName: matchedAccount.fullName,
          identifier: matchedAccount.identifier,
          role: matchedAccount.role
        };

        const session: MockAuthSession = {
          token: this.createToken(user),
          user,
          expiresIn: 86_400
        };

        this.persistSession(session);
        this.currentUser.set(user);

        return session;
      })
    );
  }

  logout(): void {
    sessionStorage.removeItem(this.tokenKey);
    this.currentUser.set(null);
  }

  isAuthenticated(): boolean {
    const token = sessionStorage.getItem(this.tokenKey);
    if (!token) {
      return false;
    }

    const payload = this.decodeToken(token);
    if (!payload || payload.exp * 1000 <= Date.now()) {
      this.logout();
      return false;
    }

    return Boolean(this.currentUser());
  }

  getUser(): MockAuthUser | null {
    return this.currentUser();
  }

  getCurrentRole(): MockAuthRole | null {
    return this.currentUser()?.role ?? null;
  }

  private restoreUser(): MockAuthUser | null {
    const token = sessionStorage.getItem(this.tokenKey);
    if (!token) {
      return null;
    }

    const payload = this.decodeToken(token);
    if (!payload || payload.exp * 1000 <= Date.now()) {
      this.logout();
      return null;
    }

    return {
      id: payload.sub,
      fullName: payload.fullName,
      identifier: payload.identifier,
      role: payload.role
    };
  }

  private persistSession(session: MockAuthSession): void {
    sessionStorage.setItem(this.tokenKey, session.token);
  }

  private normalizeIdentifier(identifier: string): string {
    const trimmedValue = identifier.trim();
    return trimmedValue.includes('@') ? trimmedValue.toLowerCase() : trimmedValue.replace(/[\s().-]/g, '');
  }

  private createToken(user: MockAuthUser): string {
    const header = this.toBase64Url({ alg: 'none', typ: 'JWT' });
    const payload = this.toBase64Url({
      sub: user.id,
      fullName: user.fullName,
      identifier: user.identifier,
      role: user.role,
      exp: Math.floor(Date.now() / 1000) + 86_400
    });

    return `${header}.${payload}.mock-signature`;
  }

  private decodeToken(token: string): MockTokenPayload | null {
    const parts = token.split('.');
    if (parts.length < 2) {
      return null;
    }

    try {
      const decodedPayload = JSON.parse(this.fromBase64Url(parts[1])) as MockTokenPayload;
      return decodedPayload;
    } catch {
      return null;
    }
  }

  private toBase64Url(value: unknown): string {
    const jsonValue = JSON.stringify(value);
    const base64 = btoa(unescape(encodeURIComponent(jsonValue)));
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
  }

  private fromBase64Url(value: string): string {
    const normalizedValue = value.replace(/-/g, '+').replace(/_/g, '/');
    const paddedValue = normalizedValue + '='.repeat((4 - (normalizedValue.length % 4)) % 4);
    return decodeURIComponent(escape(atob(paddedValue)));
  }
}


export type MockAuthRole = 'director' | 'accountant' | 'administrator';

export interface MockAuthUser {
  id: string;
  fullName: string;
  identifier: string;
  role: MockAuthRole;
}

export interface MockAuthSession {
  token: string;
  user: MockAuthUser;
  expiresIn: number;
}

export interface MockTokenPayload {
  sub: string;
  fullName: string;
  identifier: string;
  role: MockAuthRole;
  exp: number;
}


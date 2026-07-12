/**
 * Auth models used by the application layer.
 * MockAuthRole and MockAuthUser bridge the backend UserData with the existing
 * navigation/guards system. MockAuthSession and MockTokenPayload were used
 * exclusively by the old mock authentication and have been removed.
 */

export type MockAuthRole = 'director' | 'accountant' | 'administrator';

export interface MockAuthUser {
  id: string;
  fullName: string;
  identifier: string;
  role: MockAuthRole;
}

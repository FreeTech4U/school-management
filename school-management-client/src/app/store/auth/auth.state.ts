import { UserData } from '../../core/models';

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated' | 'error';
export type RegistrationStatus = 'idle' | 'loading' | 'success' | 'error';

export interface AuthState {
  status: AuthStatus;
  user: UserData | null;
  error: string | null;
  registrationStatus: RegistrationStatus;
  registrationMessage: string | null;
  registrationError: string | null;
}

export const initialAuthState: AuthState = {
  status: 'idle',
  user: null,
  error: null,
  registrationStatus: 'idle',
  registrationMessage: null,
  registrationError: null
};


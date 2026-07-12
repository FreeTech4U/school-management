import { createFeature, createReducer, on } from '@ngrx/store';

import { authActions } from './auth.actions';
import { AuthState, initialAuthState } from './auth.state';

const reducer = createReducer<AuthState>(
  initialAuthState,
  // Auth status transitions
  on(authActions.initialize, authActions.loadSession, authActions.loginRequested, (state) => ({
    ...state,
    status: 'loading' as const,
    error: null
  })),
  on(authActions.logoutRequested, (state) => ({
    ...state,
    status: 'loading' as const,
    error: null
  })),
  on(authActions.loadSessionSuccess, (state, { user }) => ({
    ...state,
    status: user ? ('authenticated' as const) : ('unauthenticated' as const),
    user,
    error: null
  })),
  on(authActions.loginSucceeded, (state, { user }) => ({
    ...state,
    status: 'authenticated' as const,
    user,
    error: null
  })),
  on(authActions.loadSessionFailure, authActions.loginFailed, (state, { error }) => ({
    ...state,
    status: 'error' as const,
    error
  })),
  on(authActions.logoutCompleted, () => ({
    ...initialAuthState,
    status: 'unauthenticated' as const
  })),
  // Registration status transitions
  on(authActions.registerRequested, (state) => ({
    ...state,
    registrationStatus: 'loading' as const,
    registrationError: null,
    registrationMessage: null
  })),
  on(authActions.registerSucceeded, (state, { message }) => ({
    ...state,
    registrationStatus: 'success' as const,
    registrationMessage: message,
    registrationError: null
  })),
  on(authActions.registerFailed, (state, { error }) => ({
    ...state,
    registrationStatus: 'error' as const,
    registrationError: error,
    registrationMessage: null
  })),
  on(authActions.clearRegistrationStatus, (state) => ({
    ...state,
    registrationStatus: 'idle' as const,
    registrationMessage: null,
    registrationError: null
  }))
);

export const authFeature = createFeature({
  name: 'auth',
  reducer
});

export const authFeatureKey = authFeature.name;
export const authReducer = authFeature.reducer;


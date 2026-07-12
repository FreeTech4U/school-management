import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';

import { LoginCredentials } from '../../features/auth/models/login-credentials.model';
import { OnboardingRequest } from '../../core/models/api';
import { authActions } from './auth.actions';
import {
  selectAuthError,
  selectAuthStatus,
  selectAuthUser,
  selectIsAuthenticated,
  selectIsLoadingAuth,
  selectIsLoadingRegister,
  selectIsRegisterSuccess,
  selectRegisterError
} from './auth.selectors';

@Injectable({ providedIn: 'root' })
export class AuthFacade {
  private readonly store = inject(Store);

  // Auth state
  readonly status$ = this.store.select(selectAuthStatus);
  readonly user$ = this.store.select(selectAuthUser);
  readonly isAuthenticated$ = this.store.select(selectIsAuthenticated);
  readonly isLoadingAuth$ = this.store.select(selectIsLoadingAuth);
  readonly authError$ = this.store.select(selectAuthError);

  // Registration state
  readonly isLoadingRegister$ = this.store.select(selectIsLoadingRegister);
  readonly isRegisterSuccess$ = this.store.select(selectIsRegisterSuccess);
  readonly registerError$ = this.store.select(selectRegisterError);

  initialize(): void {
    this.store.dispatch(authActions.initialize());
  }

  requestLogin(credentials: LoginCredentials): void {
    this.store.dispatch(authActions.loginRequested({ credentials }));
  }

  requestLogout(): void {
    this.store.dispatch(authActions.logoutRequested());
  }

  requestRegister(request: OnboardingRequest): void {
    this.store.dispatch(authActions.registerRequested({ request }));
  }

  clearRegistrationStatus(): void {
    this.store.dispatch(authActions.clearRegistrationStatus());
  }
}

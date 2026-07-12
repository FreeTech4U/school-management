import { createSelector } from '@ngrx/store';

import { authFeature } from './auth.reducer';

export const {
  selectAuthState,
  selectStatus: selectAuthStatus,
  selectUser: selectAuthUser,
  selectError: selectAuthError,
  selectRegistrationStatus: selectRegisterStatus,
  selectRegistrationMessage: selectRegisterMessage,
  selectRegistrationError: selectRegisterError
} = authFeature;

export const selectIsAuthenticated = createSelector(selectAuthStatus, (status) => status === 'authenticated');

export const selectIsAuthenticatedAuth = selectIsAuthenticated;

export const selectIsLoadingAuth = createSelector(selectAuthStatus, (status) => status === 'loading');

export const selectIsLoadingRegister = createSelector(selectRegisterStatus, (status) => status === 'loading');

export const selectIsRegisterSuccess = createSelector(selectRegisterStatus, (status) => status === 'success');


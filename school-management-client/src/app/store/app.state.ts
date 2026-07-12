import { authFeatureKey, AuthState } from './auth';

export interface AppState {
  [authFeatureKey]: AuthState;
}


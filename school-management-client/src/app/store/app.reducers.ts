import { ActionReducerMap } from '@ngrx/store';

import { authFeatureKey, authReducer } from './auth';
import { AppState } from './app.state';

export const appReducers: ActionReducerMap<AppState> = {
  [authFeatureKey]: authReducer
};


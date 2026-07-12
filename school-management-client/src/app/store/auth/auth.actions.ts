import { createActionGroup, emptyProps, props } from '@ngrx/store';

import { LoginCredentials } from '../../features/auth/models/login-credentials.model';
import { OnboardingRequest, UserData } from '../../core/models';

export const authActions = createActionGroup({
  source: 'Auth',
  events: {
    Initialize: emptyProps(),
    'Load Session': emptyProps(),
    'Load Session Success': props<{ user: UserData | null }>(),
    'Load Session Failure': props<{ error: string }>(),
    'Login Requested': props<{ credentials: LoginCredentials }>(),
    'Login Succeeded': props<{ user: UserData }>(),
    'Login Failed': props<{ error: string }>(),
    'Logout Requested': emptyProps(),
    'Logout Completed': emptyProps(),
    'Register Requested': props<{ request: OnboardingRequest }>(),
    'Register Succeeded': props<{ message: string }>(),
    'Register Failed': props<{ error: string }>(),
    'Clear Registration Status': emptyProps()
  }
});


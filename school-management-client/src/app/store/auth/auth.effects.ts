import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, of, switchMap } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { authActions } from './auth.actions';

@Injectable()
export class AuthEffects {
  private readonly actions$ = inject(Actions);
  private readonly authService = inject(AuthService);

  readonly initialize$ = createEffect(() => this.actions$.pipe(ofType(authActions.initialize), map(() => authActions.loadSession())));

  readonly loadSession$ = createEffect(() =>
    this.actions$.pipe(
      ofType(authActions.loadSession),
      switchMap(() =>
        this.authService.loadSession().pipe(
          map((user) => authActions.loadSessionSuccess({ user })),
          catchError((error: Error) => of(authActions.loadSessionFailure({ error: error.message })))
        )
      )
    )
  );

  readonly login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(authActions.loginRequested),
      switchMap(({ credentials }) =>
        this.authService.login(credentials).pipe(
          map((session) => authActions.loginSucceeded({ user: session.user })),
          catchError((error: Error) => of(authActions.loginFailed({ error: error.message })))
        )
      )
    )
  );

  readonly logout$ = createEffect(() =>
    this.actions$.pipe(
      ofType(authActions.logoutRequested),
      switchMap(() =>
        this.authService.requestLogout().pipe(
          map(() => authActions.logoutCompleted()),
          catchError(() => of(authActions.logoutCompleted()))
        )
      )
    )
  );

  readonly register$ = createEffect(() =>
    this.actions$.pipe(
      ofType(authActions.registerRequested),
      switchMap(({ request }) =>
        this.authService.register(request).pipe(
          map((response) => authActions.registerSucceeded({ message: response.message })),
          catchError((error: Error) => of(authActions.registerFailed({ error: error.message })))
        )
      )
    )
  );
}

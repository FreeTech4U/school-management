import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

const AUTH_RETRY_CONTEXT = new HttpContextToken<boolean>(() => false);

function isLoginOrRefreshRequest(url: string): boolean {
  return url.includes('/api/v1/auth/login') || url.includes('/api/v1/auth/refresh');
}

function withBearerToken(request: Parameters<HttpInterceptorFn>[0], token: string, markRetried = false) {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    },
    context: markRetried ? request.context.set(AUTH_RETRY_CONTEXT, true) : request.context
  });
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);

  if (isLoginOrRefreshRequest(request.url)) {
    return next(request);
  }

  const accessToken = authService.getAccessToken();
  const authorizedRequest = accessToken ? withBearerToken(request, accessToken) : request;

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      const shouldRefresh =
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !request.context.get(AUTH_RETRY_CONTEXT) &&
        !isLoginOrRefreshRequest(request.url);

      if (!shouldRefresh) {
        return throwError(() => error);
      }

      return authService.refreshAccessToken().pipe(
        switchMap((newToken) => next(withBearerToken(request, newToken, true))),
        catchError((refreshError) => {
          authService.clearSession();
          return throwError(() => refreshError);
        })
      );
    })
  );
};


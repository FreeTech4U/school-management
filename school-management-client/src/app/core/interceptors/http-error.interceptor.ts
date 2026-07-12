import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { HttpErrorService } from '../services/http-error.service';

export const httpErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const httpErrorService = inject(HttpErrorService);

  return next(request).pipe(catchError((error) => throwError(() => httpErrorService.normalize(error))));
};


import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class HttpErrorService {
  normalize(error: unknown): Error {
    if (error instanceof Error && !(error instanceof HttpErrorResponse)) {
      return error;
    }

    if (error instanceof HttpErrorResponse) {
      return new Error(this.getMessageFromHttpError(error));
    }

    return new Error('UNEXPECTED_ERROR');
  }

  private getMessageFromHttpError(error: HttpErrorResponse): string {
    const errorBody = error.error as Partial<{ message: string; error: string }> | string | null;

    if (typeof errorBody === 'string' && errorBody.trim()) {
      return errorBody;
    }

    if (errorBody && typeof errorBody === 'object') {
      if (typeof errorBody.message === 'string' && errorBody.message.trim()) {
        return errorBody.message;
      }

      if (typeof errorBody.error === 'string' && errorBody.error.trim()) {
        return errorBody.error;
      }
    }

    switch (error.status) {
      case 0:
        return 'NETWORK_ERROR';
      case 400:
        return 'BAD_REQUEST';
      case 401:
        return 'UNAUTHORIZED';
      case 403:
        return 'FORBIDDEN';
      case 404:
        return 'NOT_FOUND';
      case 500:
        return 'SERVER_ERROR';
      default:
        return 'HTTP_ERROR';
    }
  }
}


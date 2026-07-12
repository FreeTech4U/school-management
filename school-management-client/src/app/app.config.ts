import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, APP_INITIALIZER, importProvidersFrom, isDevMode, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TRANSLATE_HTTP_LOADER_CONFIG, TranslateHttpLoader } from '@ngx-translate/http-loader';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';

import { LanguageService } from './core/i18n/language.service';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';
import { routes } from './app.routes';
import { appEffects, appReducers } from './store';

const isDevelopment = isDevMode();

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideAnimations(),
    provideHttpClient(withInterceptors([httpErrorInterceptor, authInterceptor])),
    provideRouter(routes),
    provideStore(appReducers),
    provideEffects(...appEffects),
    ...(isDevelopment
      ? [
          provideStoreDevtools({
            maxAge: 25,
            logOnly: !isDevelopment,
            autoPause: true
          })
        ]
      : []),
    providePrimeNG({
      theme: {
        preset: Aura
      }
    }),
    {
      provide: APP_INITIALIZER,
      useFactory: (_languageService: LanguageService) => () => void 0,
      deps: [LanguageService],
      multi: true
    },
    {
      provide: TRANSLATE_HTTP_LOADER_CONFIG,
      useValue: {
        prefix: '/i18n/',
        suffix: '.json'
      }
    },
    importProvidersFrom(
      TranslateModule.forRoot({
        fallbackLang: 'fr',
        loader: {
          provide: TranslateLoader,
          useClass: TranslateHttpLoader
        }
      })
    )
  ]
};

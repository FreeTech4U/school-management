import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

type AppLanguage = 'fr' | 'en';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly storageKey = 'app.language';
  private readonly supportedLanguages: AppLanguage[] = ['fr', 'en'];

  constructor(private readonly translate: TranslateService) {
    this.translate.addLangs(this.supportedLanguages);
    this.translate.setFallbackLang('fr');

    const browserLanguage = (this.translate.getBrowserLang() ?? 'fr') as AppLanguage;
    const storedLanguage = localStorage.getItem(this.storageKey) as AppLanguage | null;
    const initialLanguage = this.supportedLanguages.includes(storedLanguage ?? browserLanguage)
      ? (storedLanguage ?? browserLanguage)
      : 'fr';

    this.use(initialLanguage);
  }

  get currentLanguage(): AppLanguage {
    return (this.translate.currentLang as AppLanguage) || 'fr';
  }

  use(language: AppLanguage): void {
    this.translate.use(language);
    localStorage.setItem(this.storageKey, language);
    document.documentElement.lang = language;
  }
}


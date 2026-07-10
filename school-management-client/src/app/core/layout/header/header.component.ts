import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../i18n/language.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  constructor(private readonly languageService: LanguageService) {}

  protected changeLanguage(language: 'fr' | 'en'): void {
    this.languageService.use(language);
  }

  protected get currentLanguage(): 'fr' | 'en' {
    return this.languageService.currentLanguage;
  }
}


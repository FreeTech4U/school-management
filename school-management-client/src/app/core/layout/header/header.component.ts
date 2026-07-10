import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../auth/auth.service';
import { LanguageService } from '../../i18n/language.service';
import { NavigationService } from '../../navigation/navigation.service';
import { NavItem } from '../../navigation/models/nav-item.model';

@Component({
  selector: 'app-header',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  constructor(
    private readonly languageService: LanguageService,
    private readonly authService: AuthService,
    private readonly navigationService: NavigationService,
    private readonly router: Router
  ) {}

  protected changeLanguage(language: 'fr' | 'en'): void {
    this.languageService.use(language);
  }

  protected get currentLanguage(): 'fr' | 'en' {
    return this.languageService.currentLanguage;
  }

  protected get isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  protected get currentUser() {
    return this.authService.currentUser();
  }

  protected get menuSections(): NavItem[] {
    return this.navigationService.getMenuForRole(this.authService.getCurrentRole());
  }

  protected logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }
}


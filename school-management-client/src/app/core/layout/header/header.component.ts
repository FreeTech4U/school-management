import { Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { Router, RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';

import { AuthService } from '../../auth/auth.service';
import { LanguageService } from '../../i18n/language.service';
import { NavigationService } from '../../navigation/navigation.service';
import { NavItem } from '../../navigation/models/nav-item.model';
import { authActions } from '../../../store/auth';

@Component({
  selector: 'app-header',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  private readonly languageService = inject(LanguageService);
  private readonly authService = inject(AuthService);
  private readonly navigationService = inject(NavigationService);
  private readonly router = inject(Router);
  private readonly store = inject(Store);

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
    this.store.dispatch(authActions.logoutRequested());
    void this.router.navigate(['/']);
  }
}

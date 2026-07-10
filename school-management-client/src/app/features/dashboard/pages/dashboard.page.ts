import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../../core/auth/auth.service';
import { FooterComponent } from '../../../core/layout/footer/footer.component';
import { HeaderComponent } from '../../../core/layout/header/header.component';

@Component({
  selector: 'app-dashboard-page',
  imports: [HeaderComponent, FooterComponent, RouterLink, TranslatePipe],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.css'
})
export class DashboardPage {
  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  protected get user() {
    return this.authService.currentUser();
  }

  protected get roleLabelKey(): string {
    return `roles.${this.user?.role ?? 'administrator'}`;
  }

  protected logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }
}



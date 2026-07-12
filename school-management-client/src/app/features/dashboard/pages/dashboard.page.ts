import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngrx/store';

import { authActions, selectAuthUser } from '../../../store/auth';
import { AuthService } from '../../../core/auth/auth.service';
import { FooterComponent } from '../../../core/layout/footer/footer.component';
import { HeaderComponent } from '../../../core/layout/header/header.component';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-dashboard-page',
  imports: [HeaderComponent, FooterComponent, RouterLink, TranslatePipe],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.css'
})
export class DashboardPage {
  private readonly store = inject(Store);
  private readonly router = inject(Router);
  // authService still needed for currentUser signal consumed by Header
  private readonly authService = inject(AuthService);

  protected readonly user = toSignal(this.store.select(selectAuthUser));

  protected get roleLabelKey(): string {
    return `roles.${this.authService.getCurrentRole() ?? 'administrator'}`;
  }

  protected logout(): void {
    this.store.dispatch(authActions.logoutRequested());
    void this.router.navigate(['/']);
  }
}

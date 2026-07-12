import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { filter } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';

import { authActions, selectAuthError, selectIsAuthenticatedAuth, selectIsLoadingAuth } from '../../../../store/auth';
import { FooterComponent } from '../../../../core/layout/footer/footer.component';
import { HeaderComponent } from '../../../../core/layout/header/header.component';
import { LoginCredentials } from '../../models/login-credentials.model';

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    AsyncPipe,
    HeaderComponent,
    FooterComponent,
    ButtonModule,
    InputTextModule,
    MessageModule
  ],
  templateUrl: './login.page.html',
  styleUrl: './login.page.css'
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly store = inject(Store);

  protected submitted = false;
  protected readonly loginForm;

  protected readonly isLoading$ = this.store.select(selectIsLoadingAuth);
  protected readonly authError$ = this.store.select(selectAuthError);

  constructor() {
    this.loginForm = this.fb.nonNullable.group({
      identifier: ['', [Validators.required, LoginPage.emailOrPhoneValidator]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });

    this.store.select(selectIsAuthenticatedAuth)
      .pipe(
        filter(Boolean),
        takeUntilDestroyed()
      )
      .subscribe(() => void this.router.navigate(['/dashboard']));

    this.isLoading$
      .pipe(takeUntilDestroyed())
      .subscribe((loading) => {
        loading ? this.loginForm.disable() : this.loginForm.enable();
      });
  }

  protected onSubmit(): void {
    this.submitted = true;

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const credentials: LoginCredentials = this.loginForm.getRawValue();
    this.store.dispatch(authActions.loginRequested({ credentials }));
  }

  protected isInvalid(controlName: 'identifier' | 'password'): boolean {
    const control = this.loginForm.controls[controlName];
    return control.invalid && (control.touched || this.submitted);
  }

  protected static emailOrPhoneValidator(control: AbstractControl): ValidationErrors | null {
    const rawValue = String(control.value ?? '').trim();

    if (!rawValue) {
      return null;
    }

    const normalizedPhone = rawValue.replace(/[\s().-]/g, '');
    const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(rawValue);
    const isPhone = /^\+?[0-9]{8,15}$/.test(normalizedPhone);

    return isEmail || isPhone ? null : { invalidIdentifier: true };
  }
}

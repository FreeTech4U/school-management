import { Component, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { FooterComponent } from '../../../../core/layout/footer/footer.component';
import { HeaderComponent } from '../../../../core/layout/header/header.component';
import { LoginCredentials } from '../../models/login-credentials.model';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, HeaderComponent, FooterComponent],
  templateUrl: './login.page.html',
  styleUrl: './login.page.css'
})
export class LoginPage {
  protected readonly submitted = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly loginForm;

  constructor(private readonly fb: FormBuilder) {
    this.loginForm = this.fb.nonNullable.group({
      identifier: ['', [Validators.required, LoginPage.emailOrPhoneValidator]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  protected onSubmit(): void {
    this.submitted.set(true);

    if (this.loginForm.invalid || this.isSubmitting()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    // This is a placeholder until backend auth integration is connected.
    const credentials: LoginCredentials = this.loginForm.getRawValue();
    console.info('Login payload ready', credentials);

    setTimeout(() => {
      this.isSubmitting.set(false);
    }, 600);
  }

  protected isInvalid(controlName: 'identifier' | 'password'): boolean {
    const control = this.loginForm.controls[controlName];
    return control.invalid && (control.touched || this.submitted());
  }

  private static emailOrPhoneValidator(control: AbstractControl): ValidationErrors | null {
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



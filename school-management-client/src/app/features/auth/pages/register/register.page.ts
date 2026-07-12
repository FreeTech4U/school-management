import { Component, inject, signal } from '@angular/core';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { filter } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';

import { authActions, selectRegisterError, selectIsLoadingRegister, selectIsRegisterSuccess } from '../../../../store/auth';
import { FooterComponent } from '../../../../core/layout/footer/footer.component';
import { HeaderComponent } from '../../../../core/layout/header/header.component';
import { OnboardingRequest } from '../../../../core/models/api';

@Component({
  selector: 'app-register-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    HeaderComponent,
    FooterComponent,
    ButtonModule,
    InputTextModule,
    MessageModule
  ],
  templateUrl: './register.page.html',
  styleUrl: './register.page.css'
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly store = inject(Store);

  protected readonly submitted = signal(false);
  protected readonly registerForm;

  protected readonly isLoading = toSignal(this.store.select(selectIsLoadingRegister), { initialValue: false });
  protected readonly registerError = toSignal(this.store.select(selectRegisterError), { initialValue: null });
  protected readonly registerSuccess = toSignal(this.store.select(selectIsRegisterSuccess), { initialValue: false });

  constructor() {
    this.registerForm = this.fb.nonNullable.group({
      schoolName: ['', [Validators.required, Validators.minLength(3)]],
      slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
      city: ['', [Validators.required, Validators.minLength(2)]],
      countryCode: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
      directorFirstName: ['', [Validators.required, Validators.minLength(2)]],
      directorLastName: ['', [Validators.required, Validators.minLength(2)]],
      directorPassword: ['', [Validators.required, Validators.minLength(8)]],
      planCode: ['', [Validators.required]],
      currency: ['', [Validators.required]],
      timezone: ['', [Validators.required]]
    });

    // Redirect on successful registration — runs outside the render cycle
    this.store.select(selectIsRegisterSuccess)
      .pipe(
        filter(Boolean),
        takeUntilDestroyed()
      )
      .subscribe(() => {
        this.store.dispatch(authActions.clearRegistrationStatus());
        void this.router.navigate(['/login']);
      });

    // Disable the entire form during loading — avoids [attr.disabled] on pInputText (NG0600)
    this.store.select(selectIsLoadingRegister)
      .pipe(takeUntilDestroyed())
      .subscribe((loading) => {
        loading ? this.registerForm.disable() : this.registerForm.enable();
      });
  }

  protected onSubmit(): void {
    this.submitted.set(true);

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const request: OnboardingRequest = this.registerForm.getRawValue();
    this.store.dispatch(authActions.registerRequested({ request }));
  }

  protected isInvalid(controlName: string): boolean {
    const control = this.registerForm.get(controlName);
    return control ? control.invalid && (control.touched || this.submitted()) : false;
  }
}

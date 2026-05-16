import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '@core/services/auth.service';
import { PasswordInputComponent } from '@shared/components/password-input/password-input.component';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PasswordInputComponent],
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly erroLogin = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
    manterConectado: [false],
  });

  entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.erroLogin.set(null);
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        const rotaDeRedirect = this.activatedRoute.snapshot.queryParamMap.get('redirectTo') ?? '/';
        this.router.navigateByUrl(rotaDeRedirect);
      },
      error: (httpError: HttpErrorResponse) => {
        this.loading.set(false);
        const detalheBackend = httpError?.error?.detail;
        if (httpError.status === 401 || httpError.status === 400) {
          this.erroLogin.set('Email ou senha incorretos.');
        } else if (httpError.status === 429) {
          this.erroLogin.set('Muitas tentativas. Aguarde alguns minutos e tente novamente.');
        } else {
          this.erroLogin.set(detalheBackend ?? 'Não foi possível autenticar. Tente novamente.');
        }
      },
    });
  }
}

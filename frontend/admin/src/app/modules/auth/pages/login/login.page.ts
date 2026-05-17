import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';
import { ForbiddenLoginError } from '@core/models/errors';
import { ToastService } from '@shared/services/toast.service';
import { ToastStackComponent } from '@shared/components/toast-stack/toast-stack.component';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ToastStackComponent],
  templateUrl: './login.page.html',
})
export class LoginPage {
  private readonly auth = inject(AdminAuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly submetendo = signal(false);

  readonly formulario = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
  });

  submeter(): void {
    if (this.formulario.invalid || this.submetendo()) {
      return;
    }
    this.submetendo.set(true);
    const valor = this.formulario.getRawValue();
    this.auth.login({ email: valor.email, senha: valor.senha }).subscribe({
      next: () => {
        this.submetendo.set(false);
        this.router.navigateByUrl('/dashboard');
      },
      error: (erro) => {
        this.submetendo.set(false);
        if (erro instanceof ForbiddenLoginError) {
          this.toast.error('Sem permissão para acessar o admin.');
        } else if (erro instanceof HttpErrorResponse && erro.status === 401) {
          this.toast.error('E-mail ou senha inválidos.');
        } else {
          this.toast.error('Erro ao entrar. Tente novamente.');
        }
      },
    });
  }
}

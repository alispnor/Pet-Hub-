import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-md px-6 py-16">
      <div class="surface">
        <h1 class="text-2xl">Entrar na sua conta</h1>
        <p class="mt-2 text-sm text-graphite-600">
          Acesse pedidos, endereços e pets cadastrados.
        </p>

        <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="onSubmit()">
          <div>
            <label class="label" for="email">Email</label>
            <input id="email" type="email" autocomplete="email"
                   class="input"
                   formControlName="email"
                   placeholder="seu@email.com" />
            <p *ngIf="form.controls.email.touched && form.controls.email.invalid"
               class="form-error">
              Informe um email válido.
            </p>
          </div>
          <div>
            <label class="label" for="senha">Senha</label>
            <input id="senha" type="password" autocomplete="current-password"
                   class="input"
                   formControlName="senha"
                   placeholder="••••••••" />
            <p *ngIf="form.controls.senha.touched && form.controls.senha.invalid"
               class="form-error">
              A senha deve ter no mínimo 8 caracteres.
            </p>
          </div>

          <p *ngIf="error()" class="form-error">{{ error() }}</p>

          <button type="submit" class="btn-primary w-full"
                  [disabled]="form.invalid || loading()">
            <span *ngIf="!loading()">Entrar</span>
            <span *ngIf="loading()">Entrando…</span>
          </button>
        </form>

        <p class="mt-6 text-sm text-graphite-600">
          Ainda não tem conta?
          <a routerLink="/cadastro" class="font-medium text-coral-500">Criar conta</a>
        </p>
      </div>
    </section>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        const redirect = this.route.snapshot.queryParamMap.get('redirectTo') ?? '/';
        this.router.navigateByUrl(redirect);
      },
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        const detail = e?.error?.detail;
        if (e.status === 401 || e.status === 400) {
          this.error.set('Email ou senha incorretos.');
        } else if (e.status === 429) {
          this.error.set('Muitas tentativas. Aguarde alguns minutos e tente novamente.');
        } else {
          this.error.set(detail ?? 'Não foi possível autenticar. Tente novamente.');
        }
      },
    });
  }
}

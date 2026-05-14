import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../core/services/auth.service';

/**
 * Validação client-side de senha: 8+ chars, ao menos 1 letra maiúscula,
 * 1 dígito e 1 caractere especial. A validação canônica é server-side;
 * isto é só UX para reduzir round-trips.
 */
function senhaStrong(control: AbstractControl): ValidationErrors | null {
  const v: string = control.value ?? '';
  const hasUpper = /[A-Z]/.test(v);
  const hasDigit = /\d/.test(v);
  const hasSpecial = /[^A-Za-z0-9]/.test(v);
  if (!hasUpper || !hasDigit || !hasSpecial) {
    return { senhaFraca: true };
  }
  return null;
}

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-md px-6 py-16">
      <div class="surface">
        <h1 class="text-2xl">Criar sua conta</h1>
        <p class="mt-2 text-sm text-graphite-600">
          Em segundos você já pode pedir, salvar pets e acompanhar entregas.
        </p>

        <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="onSubmit()">
          <div>
            <label class="label" for="nome">Nome completo</label>
            <input id="nome" type="text" autocomplete="name"
                   class="input"
                   formControlName="nome"
                   placeholder="Ex.: Ana Costa" />
            <p *ngIf="form.controls.nome.touched && form.controls.nome.invalid"
               class="form-error">
              Nome deve ter entre 2 e 200 caracteres.
            </p>
          </div>

          <div>
            <label class="label" for="email">Email</label>
            <input id="email" type="email" autocomplete="email"
                   class="input"
                   formControlName="email"
                   placeholder="voce@email.com" />
            <p *ngIf="form.controls.email.touched && form.controls.email.invalid"
               class="form-error">
              Informe um email válido.
            </p>
          </div>

          <div>
            <label class="label" for="telefone">Telefone (opcional)</label>
            <input id="telefone" type="tel" autocomplete="tel"
                   class="input"
                   formControlName="telefone"
                   placeholder="(11) 99999-0000" />
          </div>

          <div>
            <label class="label" for="senha">Senha</label>
            <input id="senha" type="password" autocomplete="new-password"
                   class="input"
                   formControlName="senha"
                   placeholder="Mínimo 8 caracteres" />
            <p *ngIf="form.controls.senha.touched && form.controls.senha.errors?.['required']"
               class="form-error">A senha é obrigatória.</p>
            <p *ngIf="form.controls.senha.touched && form.controls.senha.errors?.['minlength']"
               class="form-error">Mínimo 8 caracteres.</p>
            <p *ngIf="form.controls.senha.touched && form.controls.senha.errors?.['senhaFraca']"
               class="form-error">
              Use ao menos uma letra maiúscula, um número e um caractere especial.
            </p>
          </div>

          <p *ngIf="error()" class="form-error">{{ error() }}</p>

          <button type="submit" class="btn-primary w-full"
                  [disabled]="form.invalid || loading()">
            <span *ngIf="!loading()">Criar conta</span>
            <span *ngIf="loading()">Criando…</span>
          </button>
        </form>

        <p class="mt-6 text-sm text-graphite-600">
          Já tem conta?
          <a routerLink="/login" class="font-medium text-coral-500">Entrar</a>
        </p>
      </div>
    </section>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    telefone: [''],
    senha: ['', [Validators.required, Validators.minLength(8), senhaStrong]],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    const payload = {
      nome: v.nome.trim(),
      email: v.email.trim().toLowerCase(),
      senha: v.senha,
      telefone: v.telefone?.trim() || null,
    };
    this.auth.registerCliente(payload).subscribe({
      next: () => {
        // Após registrar, faz login automático para abrir sessão.
        this.auth.login({ email: payload.email, senha: payload.senha }).subscribe({
          next: () => {
            this.loading.set(false);
            this.router.navigateByUrl('/');
          },
          error: () => {
            this.loading.set(false);
            this.error.set('Conta criada, mas houve um problema ao entrar. Tente novamente.');
          },
        });
      },
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        const detail = e?.error?.detail;
        if (e.status === 409) {
          this.error.set('Já existe uma conta com esse email.');
        } else if (e.status === 400 && e?.error?.errors) {
          const msgs = (e.error.errors as Array<{ field: string; message: string }>)
            .map((er) => `${er.field}: ${er.message}`)
            .join(' · ');
          this.error.set(msgs);
        } else {
          this.error.set(detail ?? 'Não foi possível criar sua conta. Tente novamente.');
        }
      },
    });
  }
}

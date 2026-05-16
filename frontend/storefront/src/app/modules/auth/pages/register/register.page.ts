import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '@core/services/auth.service';
import { PasswordInputComponent } from '@shared/components/password-input/password-input.component';

/**
 * Validação client-side de senha: 8+ chars, ao menos 1 letra maiúscula,
 * 1 dígito e 1 caractere especial. A validação canônica é server-side;
 * isto é só UX para reduzir round-trips.
 */
function validadorSenhaForte(controle: AbstractControl): ValidationErrors | null {
  const valorSenha: string = controle.value ?? '';
  const temMaiuscula = /[A-Z]/.test(valorSenha);
  const temDigito = /\d/.test(valorSenha);
  const temCaractereEspecial = /[^A-Za-z0-9]/.test(valorSenha);
  if (!temMaiuscula || !temDigito || !temCaractereEspecial) {
    return { senhaFraca: true };
  }
  return null;
}

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PasswordInputComponent],
  templateUrl: './register.page.html',
  styleUrls: ['./register.page.scss'],
})
export class RegisterPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly erroCadastro = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(200)]],
    telefone: [''],
    senha: ['', [Validators.required, Validators.minLength(8), validadorSenhaForte]],
  });

  cadastrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.erroCadastro.set(null);
    const valoresFormulario = this.form.getRawValue();
    const payloadCadastro = {
      nome: valoresFormulario.nome.trim(),
      email: valoresFormulario.email.trim().toLowerCase(),
      senha: valoresFormulario.senha,
      telefone: valoresFormulario.telefone?.trim() || null,
    };
    this.authService.registerCliente(payloadCadastro).subscribe({
      next: () => {
        // Após registrar, faz login automático para abrir sessão.
        this.authService.login({
          email: payloadCadastro.email,
          senha: payloadCadastro.senha,
        }).subscribe({
          next: () => {
            this.loading.set(false);
            this.router.navigateByUrl('/');
          },
          error: () => {
            this.loading.set(false);
            this.erroCadastro.set('Conta criada, mas houve um problema ao entrar. Tente novamente.');
          },
        });
      },
      error: (httpError: HttpErrorResponse) => {
        this.loading.set(false);
        const detalheBackend = httpError?.error?.detail;
        if (httpError.status === 409) {
          this.erroCadastro.set('Já existe uma conta com esse email.');
        } else if (httpError.status === 400 && httpError?.error?.errors) {
          const listaErrosCampo = httpError.error.errors as Array<{ field: string; message: string }>;
          const mensagensConcatenadas = listaErrosCampo
            .map((erroCampo) => `${erroCampo.field}: ${erroCampo.message}`)
            .join(' · ');
          this.erroCadastro.set(mensagensConcatenadas);
        } else {
          this.erroCadastro.set(detalheBackend ?? 'Não foi possível criar sua conta. Tente novamente.');
        }
      },
    });
  }
}

import { Component, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ShippingService } from '@shared/services/shipping.service';
import { OpcaoFrete } from '@shared/models/shipping';

/**
 * Mini-calculadora de frete usada nas páginas de detalhe / carrinho.
 * Aceita CEP com ou sem máscara; envia 8 dígitos para o backend.
 *
 * Segurança: o input é validado client-side (regex 8 dígitos) ANTES de chamar
 * a API. O backend revalida com @Pattern — defesa em profundidade.
 */
@Component({
  selector: 'app-shipping-calculator',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './shipping-calculator.component.html',
})
export class ShippingCalculatorComponent {
  sku = input.required<string>();
  qty = input(1);

  private readonly shippingService = inject(ShippingService);
  private readonly formBuilder = inject(FormBuilder);

  readonly opcoesFrete = signal<OpcaoFrete[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    cep: ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
  });

  calcularFrete(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const cepSemMascara = this.form.controls.cep.value.replace(/\D/g, '');
    this.loading.set(true);
    this.error.set(null);
    this.opcoesFrete.set([]);
    this.shippingService.calculate({
      cepDestino: cepSemMascara,
      itens: [{ sku: this.sku(), qty: this.qty() }],
    }).subscribe({
      next: (opcoesCalculadas) => {
        this.loading.set(false);
        this.opcoesFrete.set(opcoesCalculadas);
      },
      error: (httpError: HttpErrorResponse) => {
        this.loading.set(false);
        const detalheBackend = httpError?.error?.detail;
        if (httpError.status === 400 || httpError.status === 422) {
          this.error.set(detalheBackend ?? 'Não foi possível calcular para este CEP.');
        } else {
          this.error.set('Erro ao calcular frete. Tente novamente em instantes.');
        }
      },
    });
  }
}

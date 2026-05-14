import { Component, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ShippingService } from '../../../core/services/shipping.service';
import { OpcaoFrete } from '../../../core/models/shipping';

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
  template: `
    <form [formGroup]="form" (ngSubmit)="onCalcular()" class="space-y-3">
      <label class="label" for="cep">Calcular frete</label>
      <div class="flex gap-2">
        <input id="cep" type="text"
               class="input flex-1"
               inputmode="numeric"
               formControlName="cep"
               placeholder="00000-000"
               maxlength="9" />
        <button type="submit"
                class="btn-secondary"
                [disabled]="form.invalid || loading()">
          <span *ngIf="!loading()">Calcular</span>
          <span *ngIf="loading()">…</span>
        </button>
      </div>
      <p *ngIf="form.controls.cep.touched && form.controls.cep.invalid"
         class="form-error">CEP deve ter 8 dígitos.</p>
      <p *ngIf="error()" class="form-error">{{ error() }}</p>

      <ul *ngIf="opcoes().length" class="mt-4 divide-y divide-graphite-200 border-t border-graphite-200">
        <li *ngFor="let o of opcoes()" class="flex items-center justify-between py-3">
          <div>
            <p class="text-sm font-medium text-graphite-900">{{ o.servico }}</p>
            <p class="text-xs text-graphite-500">
              {{ o.transportadora }} · entrega em até {{ o.prazoDias }} dia(s) úteis
            </p>
          </div>
          <p class="text-sm font-semibold text-graphite-900">
            {{ o.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
          </p>
        </li>
      </ul>
    </form>
  `,
})
export class ShippingCalculatorComponent {
  sku = input.required<string>();
  qty = input(1);

  private readonly shipping = inject(ShippingService);
  private readonly fb = inject(FormBuilder);

  readonly opcoes = signal<OpcaoFrete[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    cep: ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
  });

  onCalcular(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const cep = this.form.controls.cep.value.replace(/\D/g, '');
    this.loading.set(true);
    this.error.set(null);
    this.opcoes.set([]);
    this.shipping.calculate({ cepDestino: cep, itens: [{ sku: this.sku(), qty: this.qty() }] }).subscribe({
      next: (opcoes) => {
        this.loading.set(false);
        this.opcoes.set(opcoes);
      },
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        const detail = e?.error?.detail;
        if (e.status === 400 || e.status === 422) {
          this.error.set(detail ?? 'Não foi possível calcular para este CEP.');
        } else {
          this.error.set('Erro ao calcular frete. Tente novamente em instantes.');
        }
      },
    });
  }
}

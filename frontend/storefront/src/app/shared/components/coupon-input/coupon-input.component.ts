import { Component, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CartCoupon } from '../../../core/models/cart';

/**
 * Input de cupom. Quando há cupom aplicado, mostra chip com `×`.
 * Cupom é só código (\w{1,30}); backend valida no preview do checkout.
 */
@Component({
  selector: 'app-coupon-input',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <ng-container *ngIf="!cupom(); else aplicado">
      <form [formGroup]="form" (ngSubmit)="onApply()" class="space-y-2">
        <label class="label" for="cupom">Cupom de desconto</label>
        <div class="flex gap-2">
          <input id="cupom" type="text"
                 class="input flex-1 uppercase"
                 formControlName="codigo"
                 placeholder="10OFF" />
          <button type="submit"
                  class="btn-secondary"
                  [disabled]="form.invalid || busy()">
            <span *ngIf="!busy()">Aplicar</span>
            <span *ngIf="busy()">…</span>
          </button>
        </div>
        <p *ngIf="error()" class="form-error">{{ error() }}</p>
      </form>
    </ng-container>
    <ng-template #aplicado>
      <div class="flex items-center justify-between rounded border border-graphite-200 px-3 py-2">
        <div>
          <p class="text-sm font-medium text-graphite-900">{{ cupom()!.codigo }}</p>
          <p class="text-xs text-graphite-500">Validade verificada no checkout</p>
        </div>
        <button type="button"
                class="text-graphite-500 hover:text-coral-500"
                [disabled]="busy()"
                (click)="remove.emit()"
                aria-label="Remover cupom">×</button>
      </div>
    </ng-template>
  `,
})
export class CouponInputComponent {
  cupom = input<CartCoupon | null>(null);
  busy = input(false);
  error = input<string | null>(null);
  apply = output<string>();
  remove = output<void>();

  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[\w-]{1,30}$/)]],
  });

  onApply(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.apply.emit(this.form.controls.codigo.value.trim().toUpperCase());
  }
}

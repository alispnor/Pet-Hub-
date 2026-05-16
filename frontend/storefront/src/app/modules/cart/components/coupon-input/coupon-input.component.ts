import { Component, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CartCoupon } from '@modules/cart/models/cart';

/**
 * Input de cupom. Quando há cupom aplicado, mostra chip com `×`.
 * Cupom é só código (\w{1,30}); backend valida no preview do checkout.
 */
@Component({
  selector: 'app-coupon-input',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './coupon-input.component.html',
})
export class CouponInputComponent {
  cupom = input<CartCoupon | null>(null);
  busy = input(false);
  error = input<string | null>(null);
  apply = output<string>();
  remove = output<void>();

  private readonly formBuilder = inject(FormBuilder);
  readonly form = this.formBuilder.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[\w-]{1,30}$/)]],
  });

  aplicarCupom(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const codigoNormalizado = this.form.controls.codigo.value.trim().toUpperCase();
    this.apply.emit(codigoNormalizado);
  }
}

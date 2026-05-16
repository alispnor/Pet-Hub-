import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '@core/services/auth.service';
import { CartService } from '@modules/cart/services/cart.service';
import { CartItemRowComponent } from '@modules/cart/components/cart-item-row/cart-item-row.component';
import { CouponInputComponent } from '@modules/cart/components/coupon-input/coupon-input.component';
import { ShippingCalculatorComponent } from '@shared/components/shipping-calculator/shipping-calculator.component';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CartItemRowComponent, CouponInputComponent, ShippingCalculatorComponent],
  templateUrl: './cart.page.html',
  styleUrls: ['./cart.page.scss'],
})
export class CartPage implements OnInit {
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  readonly skuOcupado = signal<string | null>(null);
  readonly cupomEmProcessamento = signal(false);
  readonly erroCupom = signal<string | null>(null);
  readonly erroLista = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) return;
    this.cartService.load().subscribe({
      error: () => this.erroLista.set('Não foi possível carregar o carrinho.'),
    });
  }

  atualizarQty(sku: string, novaQty: number): void {
    this.skuOcupado.set(sku);
    this.erroLista.set(null);
    this.cartService.updateQty(sku, novaQty).subscribe({
      next: () => this.skuOcupado.set(null),
      error: (httpError: HttpErrorResponse) => {
        this.skuOcupado.set(null);
        this.erroLista.set(httpError.error?.detail ?? 'Não foi possível atualizar a quantidade.');
      },
    });
  }

  removerItem(sku: string): void {
    this.skuOcupado.set(sku);
    this.cartService.remove(sku).subscribe({
      next: () => this.skuOcupado.set(null),
      error: (httpError: HttpErrorResponse) => {
        this.skuOcupado.set(null);
        this.erroLista.set(httpError.error?.detail ?? 'Não foi possível remover o item.');
      },
    });
  }

  aplicarCupom(codigoCupom: string): void {
    this.cupomEmProcessamento.set(true);
    this.erroCupom.set(null);
    this.cartService.applyCoupon(codigoCupom).subscribe({
      next: () => this.cupomEmProcessamento.set(false),
      error: (httpError: HttpErrorResponse) => {
        this.cupomEmProcessamento.set(false);
        this.erroCupom.set(httpError.error?.detail ?? 'Cupom inválido.');
      },
    });
  }

  removerCupom(): void {
    this.cupomEmProcessamento.set(true);
    this.cartService.removeCoupon().subscribe({
      next: () => this.cupomEmProcessamento.set(false),
      error: () => this.cupomEmProcessamento.set(false),
    });
  }

  irParaCheckout(): void {
    this.router.navigate(['/checkout/endereco']);
  }
}

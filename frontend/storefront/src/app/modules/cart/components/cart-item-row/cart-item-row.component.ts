import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CartItem } from '@modules/cart/models/cart';

/**
 * Linha do carrinho. Stepper interno de qty desabilita botões durante a
 * requisição (o parent dispara CartService.updateQty e re-habilita ao terminar).
 */
@Component({
  selector: 'app-cart-item-row',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cart-item-row.component.html',
})
export class CartItemRowComponent {
  item = input.required<CartItem>();
  busy = input(false);
  updateQty = output<{ sku: string; qty: number }>();
  remove = output<string>();

  incrementarQty(): void {
    this.updateQty.emit({ sku: this.item().sku, qty: this.item().qty + 1 });
  }

  decrementarQty(): void {
    const novaQty = this.item().qty - 1;
    if (novaQty < 1) {
      this.remove.emit(this.item().sku);
    } else {
      this.updateQty.emit({ sku: this.item().sku, qty: novaQty });
    }
  }
}

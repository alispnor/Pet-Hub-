import { Component, computed, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CartItem } from '../../../core/models/cart';

/**
 * Linha do carrinho. Stepper interno de qty desabilita botões durante a
 * requisição (o parent dispara CartService.updateQty e re-habilita ao terminar).
 */
@Component({
  selector: 'app-cart-item-row',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="flex gap-4 border-b border-graphite-200 py-4">
      <div class="h-24 w-24 flex-shrink-0 overflow-hidden rounded bg-graphite-100">
        <img *ngIf="item().imagemUrl; else ph"
             [src]="item().imagemUrl!"
             [alt]="item().nome"
             class="h-full w-full object-cover" />
        <ng-template #ph>
          <div class="flex h-full w-full items-center justify-center text-xs text-graphite-400">sem imagem</div>
        </ng-template>
      </div>
      <div class="flex flex-1 flex-col justify-between">
        <div class="flex justify-between gap-4">
          <div>
            <p class="text-xs text-graphite-500">SKU {{ item().sku }}</p>
            <h3 class="text-base text-graphite-900">{{ item().nome }}</h3>
            <p class="mt-1 text-sm text-graphite-500">
              {{ item().precoUnitario | currency:'BRL':'symbol':'1.2-2':'pt-BR' }} cada
            </p>
          </div>
          <p class="text-base font-semibold text-graphite-900 whitespace-nowrap">
            {{ item().subtotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
          </p>
        </div>
        <div class="mt-3 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <button type="button" class="btn-secondary px-2 py-1 text-sm"
                    [disabled]="busy()"
                    (click)="onDec()"
                    aria-label="Diminuir quantidade">−</button>
            <span class="min-w-[2ch] text-center text-sm tabular-nums">{{ item().qty }}</span>
            <button type="button" class="btn-secondary px-2 py-1 text-sm"
                    [disabled]="busy()"
                    (click)="onInc()"
                    aria-label="Aumentar quantidade">+</button>
          </div>
          <button type="button" class="text-sm text-graphite-500 hover:text-coral-500"
                  [disabled]="busy()"
                  (click)="remove.emit(item().sku)">
            Remover
          </button>
        </div>
      </div>
    </article>
  `,
})
export class CartItemRowComponent {
  item = input.required<CartItem>();
  busy = input(false);
  updateQty = output<{ sku: string; qty: number }>();
  remove = output<string>();

  onInc(): void {
    this.updateQty.emit({ sku: this.item().sku, qty: this.item().qty + 1 });
  }
  onDec(): void {
    const next = this.item().qty - 1;
    if (next < 1) {
      this.remove.emit(this.item().sku);
    } else {
      this.updateQty.emit({ sku: this.item().sku, qty: next });
    }
  }
}

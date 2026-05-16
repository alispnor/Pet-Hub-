import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import confetti from 'canvas-confetti';

import { PlaceOrderResponse } from '@modules/checkout/models/checkout';

@Component({
  selector: 'app-checkout-success-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './success.page.html',
  styleUrls: ['./success.page.scss'],
})
export class CheckoutSuccessPage implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly numeroPedido = signal<string>('');
  readonly snapshotPedido = signal<PlaceOrderResponse | null>(null);

  ngOnInit(): void {
    this.numeroPedido.set(this.activatedRoute.snapshot.paramMap.get('numero') ?? '');
    const navigationExtras = this.router.getCurrentNavigation();
    const snapshotRecebido =
      (navigationExtras?.extras.state as { snapshot?: PlaceOrderResponse } | undefined)?.snapshot
      ?? (history.state?.snapshot as PlaceOrderResponse | undefined);
    if (snapshotRecebido) this.snapshotPedido.set(snapshotRecebido);
    this.dispararConfete();
  }

  humanizarMetodo(metodoPagamento: string): string {
    switch (metodoPagamento) {
      case 'CARTAO_CREDITO': return 'Cartão de crédito';
      case 'CARTAO_DEBITO': return 'Cartão de débito';
      case 'PIX': return 'PIX';
      case 'BOLETO': return 'Boleto';
      default: return metodoPagamento;
    }
  }

  private dispararConfete(): void {
    if (typeof window === 'undefined') return;
    confetti({
      particleCount: 200,
      spread: 90,
      origin: { y: 0.6 },
      ticks: 200,
    });
  }
}

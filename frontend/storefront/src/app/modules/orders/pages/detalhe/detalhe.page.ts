import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { OrderService } from '@modules/orders/services/order.service';
import {
  PedidoResponse,
  Timeline,
  STATUS_LABEL,
  classesBadgeStatus,
} from '@modules/orders/models/order';
import { OrderTimelineComponent } from '@modules/orders/components/order-timeline/order-timeline.component';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';

@Component({
  selector: 'app-orders-detalhe-page',
  standalone: true,
  imports: [CommonModule, RouterLink, OrderTimelineComponent, SkeletonComponent],
  templateUrl: './detalhe.page.html',
})
export class OrdersDetalhePage implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);

  readonly STATUS_LABEL: Record<string, string> = STATUS_LABEL;
  readonly classesBadge = classesBadgeStatus;

  readonly carregando = signal(true);
  readonly erro = signal(false);
  readonly pedido = signal<PedidoResponse | null>(null);
  readonly timeline = signal<Timeline | null>(null);

  ngOnInit(): void {
    const numero = this.route.snapshot.paramMap.get('numero');
    if (!numero) {
      this.erro.set(true);
      this.carregando.set(false);
      return;
    }
    this.carregar(numero);
  }

  carregar(numero: string): void {
    this.carregando.set(true);
    this.erro.set(false);
    forkJoin({
      pedido: this.orderService.detalhe(numero),
      timeline: this.orderService.timeline(numero),
    }).subscribe({
      next: ({ pedido, timeline }) => {
        this.pedido.set(pedido);
        this.timeline.set(timeline);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }
}

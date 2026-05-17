import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, takeUntil } from 'rxjs';

import { OrderService, ListarPedidosParams } from '@modules/orders/services/order.service';
import {
  PageResponse,
  PedidoResumoResponse,
  StatusPedido,
  STATUS_LABEL,
  classesBadgeStatus,
} from '@modules/orders/models/order';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';

@Component({
  selector: 'app-orders-lista-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, SkeletonComponent],
  templateUrl: './lista.page.html',
})
export class OrdersListaPage implements OnInit, OnDestroy {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  readonly STATUS_LABEL: Record<string, string> = STATUS_LABEL;
  readonly classesBadge = classesBadgeStatus;
  readonly listaStatus: StatusPedido[] = [
    'PENDENTE_PAGAMENTO', 'PAGAMENTO_APROVADO', 'PAGAMENTO_REJEITADO',
    'SEPARACAO', 'EM_TRANSPORTE', 'ENTREGUE', 'CANCELADO', 'DEVOLVIDO',
  ];

  readonly carregando = signal(true);
  readonly erro = signal(false);
  readonly pagina = signal<PageResponse<PedidoResumoResponse> | null>(null);

  readonly filtros = this.fb.nonNullable.group({
    status: '',
    dataInicio: '',
    dataFim: '',
  });

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe(parametros => {
      this.filtros.patchValue({
        status: parametros.get('status') ?? '',
        dataInicio: parametros.get('dataInicio') ?? '',
        dataFim: parametros.get('dataFim') ?? '',
      }, { emitEvent: false });
      this.carregar(Number(parametros.get('page') ?? 0));
    });

    this.filtros.valueChanges.pipe(debounceTime(250), takeUntil(this.destroy$)).subscribe(valor => {
      this.router.navigate([], {
        queryParams: {
          page: 0,
          status: valor.status || null,
          dataInicio: valor.dataInicio || null,
          dataFim: valor.dataFim || null,
        },
        queryParamsHandling: 'merge',
      });
    });
  }

  carregar(page: number): void {
    this.carregando.set(true);
    this.erro.set(false);
    const valor = this.filtros.getRawValue();
    const parametros: ListarPedidosParams = {
      page,
      size: 10,
      sort: 'criadoEm,desc',
      status: (valor.status as StatusPedido) || undefined,
      dataInicio: valor.dataInicio ? `${valor.dataInicio}T00:00:00` : undefined,
      dataFim: valor.dataFim ? `${valor.dataFim}T23:59:59` : undefined,
    };
    this.orderService.list(parametros).subscribe({
      next: pagina => { this.pagina.set(pagina); this.carregando.set(false); },
      error: () => { this.erro.set(true); this.carregando.set(false); },
    });
  }

  irParaPagina(novaPagina: number): void {
    this.router.navigate([], {
      queryParams: { page: novaPagina },
      queryParamsHandling: 'merge',
    });
  }

  limparFiltros(): void {
    this.filtros.reset({ status: '', dataInicio: '', dataFim: '' });
    this.router.navigate([], { queryParams: {} });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}

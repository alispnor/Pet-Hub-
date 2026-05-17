import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { PerfilService } from '@modules/customer/services/perfil.service';
import { AddressService } from '@modules/customer/services/address.service';
import { PaymentMethodService } from '@modules/customer/services/payment-method.service';
import { PetService } from '@modules/customer/services/pet.service';
import { OrderService } from '@modules/orders/services/order.service';
import { PerfilResponse } from '@modules/customer/models/perfil';
import { EnderecoResponse } from '@modules/customer/models/address';
import { FormaPagamentoResponse } from '@modules/customer/models/payment-method';
import { PetResponse, ESPECIES_LABEL } from '@modules/customer/models/pet';
import {
  PedidoResumoResponse,
  STATUS_LABEL,
  classesBadgeStatus,
} from '@modules/orders/models/order';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';

type EstadoCard<T> =
  | { tipo: 'loading' }
  | { tipo: 'sucesso'; dados: T }
  | { tipo: 'erro' };

@Component({
  selector: 'app-overview-page',
  standalone: true,
  imports: [CommonModule, RouterLink, SkeletonComponent],
  templateUrl: './overview.page.html',
})
export class OverviewPage implements OnInit {
  private readonly perfilService = inject(PerfilService);
  private readonly addressService = inject(AddressService);
  private readonly paymentMethodService = inject(PaymentMethodService);
  private readonly petService = inject(PetService);
  private readonly orderService = inject(OrderService);

  readonly STATUS_LABEL: Record<string, string> = STATUS_LABEL;
  readonly ESPECIES_LABEL: Record<string, string> = ESPECIES_LABEL;
  readonly classesBadge = classesBadgeStatus;

  readonly cardPerfil = signal<EstadoCard<PerfilResponse>>({ tipo: 'loading' });
  readonly cardEnderecos = signal<EstadoCard<EnderecoResponse[]>>({ tipo: 'loading' });
  readonly cardCartoes = signal<EstadoCard<FormaPagamentoResponse[]>>({ tipo: 'loading' });
  readonly cardPets = signal<EstadoCard<PetResponse[]>>({ tipo: 'loading' });
  readonly cardPedidos = signal<EstadoCard<PedidoResumoResponse[]>>({ tipo: 'loading' });

  readonly iniciais = computed(() => {
    const card = this.cardPerfil();
    if (card.tipo !== 'sucesso') return '';
    return card.dados.nome.split(' ').slice(0, 2).map(parte => parte[0] ?? '').join('').toUpperCase();
  });

  ngOnInit(): void { this.carregarTudo(); }

  carregarTudo(): void {
    this.cardPerfil.set({ tipo: 'loading' });
    this.cardEnderecos.set({ tipo: 'loading' });
    this.cardCartoes.set({ tipo: 'loading' });
    this.cardPets.set({ tipo: 'loading' });
    this.cardPedidos.set({ tipo: 'loading' });

    forkJoin({
      perfil:    this.perfilService.get().pipe(catchError(() => of(null))),
      enderecos: this.addressService.list().pipe(catchError(() => of(null))),
      cartoes:   this.paymentMethodService.list().pipe(catchError(() => of(null))),
      pets:      this.petService.list().pipe(catchError(() => of(null))),
      pedidos:   this.orderService.list({ page: 0, size: 3, sort: 'criadoEm,desc' })
                   .pipe(catchError(() => of(null))),
    }).subscribe(resultado => {
      this.cardPerfil.set(resultado.perfil ? { tipo: 'sucesso', dados: resultado.perfil } : { tipo: 'erro' });
      this.cardEnderecos.set(resultado.enderecos ? { tipo: 'sucesso', dados: resultado.enderecos } : { tipo: 'erro' });
      this.cardCartoes.set(resultado.cartoes ? { tipo: 'sucesso', dados: resultado.cartoes } : { tipo: 'erro' });
      this.cardPets.set(resultado.pets ? { tipo: 'sucesso', dados: resultado.pets } : { tipo: 'erro' });
      this.cardPedidos.set(resultado.pedidos ? { tipo: 'sucesso', dados: resultado.pedidos.content } : { tipo: 'erro' });
    });
  }

  recarregarPerfil(): void {
    this.cardPerfil.set({ tipo: 'loading' });
    this.perfilService.get().subscribe({
      next: dados => this.cardPerfil.set({ tipo: 'sucesso', dados }),
      error: () => this.cardPerfil.set({ tipo: 'erro' }),
    });
  }
  recarregarEnderecos(): void {
    this.cardEnderecos.set({ tipo: 'loading' });
    this.addressService.list().subscribe({
      next: dados => this.cardEnderecos.set({ tipo: 'sucesso', dados }),
      error: () => this.cardEnderecos.set({ tipo: 'erro' }),
    });
  }
  recarregarCartoes(): void {
    this.cardCartoes.set({ tipo: 'loading' });
    this.paymentMethodService.list().subscribe({
      next: dados => this.cardCartoes.set({ tipo: 'sucesso', dados }),
      error: () => this.cardCartoes.set({ tipo: 'erro' }),
    });
  }
  recarregarPets(): void {
    this.cardPets.set({ tipo: 'loading' });
    this.petService.list().subscribe({
      next: dados => this.cardPets.set({ tipo: 'sucesso', dados }),
      error: () => this.cardPets.set({ tipo: 'erro' }),
    });
  }
  recarregarPedidos(): void {
    this.cardPedidos.set({ tipo: 'loading' });
    this.orderService.list({ page: 0, size: 3, sort: 'criadoEm,desc' }).subscribe({
      next: pagina => this.cardPedidos.set({ tipo: 'sucesso', dados: pagina.content }),
      error: () => this.cardPedidos.set({ tipo: 'erro' }),
    });
  }
}

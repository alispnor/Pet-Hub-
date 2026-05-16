import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AddressService } from '@modules/customer/services/address.service';
import { CartService } from '@modules/cart/services/cart.service';
import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { ShippingService } from '@shared/services/shipping.service';
import { EnderecoResponse } from '@modules/customer/models/address';
import { OpcaoFrete } from '@shared/models/shipping';
import { StepperComponent } from '@modules/checkout/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-shipping-page',
  standalone: true,
  imports: [CommonModule, StepperComponent],
  templateUrl: './shipping.page.html',
  styleUrls: ['./shipping.page.scss'],
})
export class CheckoutShippingPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly cartService = inject(CartService);
  private readonly shippingService = inject(ShippingService);
  private readonly checkoutState = inject(CheckoutStateService);
  private readonly router = inject(Router);

  readonly passosCheckout = [
    { label: 'Endereço' },
    { label: 'Frete' },
    { label: 'Pagamento' },
    { label: 'Revisão' },
  ];

  readonly enderecoSelecionado = signal<EnderecoResponse | null>(null);
  readonly opcoesDisponiveis = signal<OpcaoFrete[]>([]);
  readonly codigoOpcaoSelecionada = signal<string | null>(null);
  readonly loading = signal(true);
  readonly erroCalculo = signal<string | null>(null);

  ngOnInit(): void {
    const estadoSnapshot = this.checkoutState.state();
    if (estadoSnapshot.opcaoFreteCodigo) {
      this.codigoOpcaoSelecionada.set(estadoSnapshot.opcaoFreteCodigo);
    }

    this.addressService.list().subscribe({
      next: (listaEnderecos) => {
        const enderecoEscolhido = listaEnderecos.find((endereco) => endereco.id === estadoSnapshot.enderecoEntregaId);
        if (!enderecoEscolhido) {
          this.router.navigate(['/checkout/endereco']);
          return;
        }
        this.enderecoSelecionado.set(enderecoEscolhido);
        this.calcularOpcoesFrete(enderecoEscolhido);
      },
      error: () => this.router.navigate(['/checkout/endereco']),
    });
  }

  selecionarOpcao(opcaoFrete: OpcaoFrete): void {
    this.codigoOpcaoSelecionada.set(opcaoFrete.codigo);
  }

  prosseguirParaPagamento(): void {
    const opcaoEscolhida = this.opcoesDisponiveis().find(
      (opcao) => opcao.codigo === this.codigoOpcaoSelecionada(),
    );
    if (!opcaoEscolhida) return;
    this.checkoutState.patch({
      opcaoFreteCodigo: opcaoEscolhida.codigo,
      opcaoFreteSnapshot: opcaoEscolhida,
    });
    this.router.navigate(['/checkout/pagamento']);
  }

  voltarParaEndereco(): void {
    this.router.navigate(['/checkout/endereco']);
  }

  formatarCep(cep: string): string {
    const digitos = cep.replace(/\D/g, '');
    return digitos.length === 8 ? `${digitos.slice(0, 5)}-${digitos.slice(5)}` : cep;
  }

  private calcularOpcoesFrete(enderecoEntrega: EnderecoResponse): void {
    const itensCarrinho = (this.cartService.cart()?.items ?? []).map((itemDoCarrinho) => ({
      sku: itemDoCarrinho.sku,
      qty: itemDoCarrinho.qty,
    }));
    if (!itensCarrinho.length) {
      this.router.navigate(['/carrinho']);
      return;
    }
    this.shippingService.calculate({
      cepDestino: enderecoEntrega.cep,
      itens: itensCarrinho,
    }).subscribe({
      next: (listaOpcoes) => {
        this.loading.set(false);
        this.opcoesDisponiveis.set(listaOpcoes);
        if (!this.codigoOpcaoSelecionada() && listaOpcoes.length) {
          const opcaoMaisBarata = listaOpcoes.reduce(
            (atual, candidata) => (atual.valor <= candidata.valor ? atual : candidata),
          );
          this.codigoOpcaoSelecionada.set(opcaoMaisBarata.codigo);
        }
      },
      error: (httpError: HttpErrorResponse) => {
        this.loading.set(false);
        this.erroCalculo.set(httpError.error?.detail ?? 'Não foi possível calcular o frete.');
      },
    });
  }
}

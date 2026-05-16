import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { AddressService } from '@modules/customer/services/address.service';
import { CartService } from '@modules/cart/services/cart.service';
import { CheckoutService } from '@modules/checkout/services/checkout.service';
import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { PaymentMethodService } from '@modules/customer/services/payment-method.service';
import { CheckoutPreviewResponse } from '@modules/checkout/models/checkout';
import { EnderecoResponse } from '@modules/customer/models/address';
import { FormaPagamentoResponse } from '@modules/customer/models/payment-method';
import { StepperComponent } from '@modules/checkout/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-review-page',
  standalone: true,
  imports: [CommonModule, RouterLink, StepperComponent],
  templateUrl: './review.page.html',
  styleUrls: ['./review.page.scss'],
})
export class CheckoutReviewPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly paymentMethodService = inject(PaymentMethodService);
  private readonly checkoutService = inject(CheckoutService);
  private readonly cartService = inject(CartService);
  readonly checkoutState = inject(CheckoutStateService);
  private readonly router = inject(Router);

  readonly passosCheckout = [
    { label: 'Endereço' },
    { label: 'Frete' },
    { label: 'Pagamento' },
    { label: 'Revisão' },
  ];

  readonly resumoPreview = signal<CheckoutPreviewResponse | null>(null);
  readonly enderecoEscolhido = signal<EnderecoResponse | null>(null);
  readonly metodoPagamentoEscolhido = signal<FormaPagamentoResponse | null>(null);
  readonly loading = signal(true);
  readonly erroCarregamento = signal<string | null>(null);
  readonly submetendoPedido = signal(false);
  readonly erroSubmissao = signal<string | null>(null);
  readonly dialogEstoque = signal<{ mensagem: string } | null>(null);

  ngOnInit(): void {
    const estadoSnapshot = this.checkoutState.state();
    if (!estadoSnapshot.enderecoEntregaId || !estadoSnapshot.opcaoFreteCodigo || !estadoSnapshot.formaPagamentoId) {
      this.router.navigate(['/checkout/endereco']);
      return;
    }

    this.addressService.list().subscribe((listaEnderecos) => {
      this.enderecoEscolhido.set(
        listaEnderecos.find((endereco) => endereco.id === estadoSnapshot.enderecoEntregaId) ?? null,
      );
    });
    this.paymentMethodService.list().subscribe((listaMetodos) => {
      this.metodoPagamentoEscolhido.set(
        listaMetodos.find((metodo) => metodo.id === estadoSnapshot.formaPagamentoId) ?? null,
      );
    });

    this.checkoutService.preview({
      enderecoEntregaId: estadoSnapshot.enderecoEntregaId,
      opcaoFreteCodigo: estadoSnapshot.opcaoFreteCodigo,
      cupom: this.cartService.cart()?.cupom?.codigo,
    }).subscribe({
      next: (respostaPreview) => {
        this.resumoPreview.set(respostaPreview);
        this.loading.set(false);
      },
      error: (httpError: HttpErrorResponse) => {
        this.loading.set(false);
        if (this.ehErroDeEstoque(httpError)) {
          this.dialogEstoque.set({ mensagem: httpError.error.detail });
        } else {
          this.erroCarregamento.set(httpError.error?.detail ?? 'Não foi possível calcular o pedido.');
        }
      },
    });
  }

  async finalizarCompra(): Promise<void> {
    const estadoSnapshot = this.checkoutState.state();
    if (!estadoSnapshot.enderecoEntregaId || !estadoSnapshot.enderecoCobrancaId
        || !estadoSnapshot.opcaoFreteCodigo || !estadoSnapshot.formaPagamentoId) {
      this.router.navigate(['/checkout/endereco']);
      return;
    }
    const chaveIdempotencia = this.checkoutState.ensureIdempotencyKey();
    this.submetendoPedido.set(true);
    this.erroSubmissao.set(null);
    try {
      const respostaPedido = await firstValueFrom(this.checkoutService.placeOrder({
        enderecoEntregaId: estadoSnapshot.enderecoEntregaId,
        enderecoCobrancaId: estadoSnapshot.enderecoCobrancaId,
        opcaoFreteCodigo: estadoSnapshot.opcaoFreteCodigo,
        formaPagamentoId: estadoSnapshot.formaPagamentoId,
        cupom: this.cartService.cart()?.cupom?.codigo,
        parcelas: estadoSnapshot.parcelas,
        idempotencyKey: chaveIdempotencia,
      }));
      if (respostaPedido.status === 'APPROVED') {
        this.cartService.clearLocal();
        this.checkoutState.clear();
        this.router.navigate(['/checkout/sucesso', respostaPedido.referenciaPedido], {
          state: { snapshot: respostaPedido },
        });
      } else {
        // REJECTED — gera key nova na próxima tentativa
        this.checkoutState.patch({ idempotencyKey: null });
        this.submetendoPedido.set(false);
        this.erroSubmissao.set('Pagamento recusado. Troque a forma de pagamento ou tente novamente.');
      }
    } catch (erroSubmissao) {
      this.submetendoPedido.set(false);
      const httpError = erroSubmissao as HttpErrorResponse;
      if (this.ehErroDeEstoque(httpError)) {
        this.dialogEstoque.set({ mensagem: httpError.error.detail });
        return;
      }
      // Erro de rede / 5xx — preserva idempotencyKey, permite reenvio
      this.erroSubmissao.set(httpError.error?.detail
        ?? 'Falha ao confirmar. Verifique sua conexão e tente novamente.');
    }
  }

  ajustarCarrinho(): void {
    this.dialogEstoque.set(null);
    this.router.navigate(['/carrinho']);
  }

  formatarCep(cep: string): string {
    const digitos = cep.replace(/\D/g, '');
    return digitos.length === 8 ? `${digitos.slice(0, 5)}-${digitos.slice(5)}` : cep;
  }

  /**
   * Backend devolve 422 com ProblemDetail.detail = "Estoque insuficiente para: ..."
   * (CheckoutService.validarEstoqueDisponivel) ou "Estoque insuficiente para SKU X..."
   * (InventoryService.reservar). Não há campo estruturado — detecta pelo prefixo.
   */
  private ehErroDeEstoque(httpError: HttpErrorResponse): boolean {
    if (httpError.status !== 422) return false;
    const detalheBackend: string | undefined = httpError.error?.detail;
    return typeof detalheBackend === 'string' && detalheBackend.toLowerCase().includes('estoque insufic');
  }
}

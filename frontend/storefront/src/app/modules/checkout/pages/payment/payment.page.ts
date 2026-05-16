import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { PaymentMethodService } from '@modules/customer/services/payment-method.service';
import {
  CreateFormaPagamentoRequest,
  FormaPagamentoResponse,
  TipoPagamento,
} from '@modules/customer/models/payment-method';
import { StepperComponent } from '@modules/checkout/components/stepper/stepper.component';

type AbaPagamento = 'CARTAO' | 'PIX' | 'BOLETO';

@Component({
  selector: 'app-checkout-payment-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StepperComponent],
  templateUrl: './payment.page.html',
  styleUrls: ['./payment.page.scss'],
})
export class CheckoutPaymentPage implements OnInit {
  private readonly paymentMethodService = inject(PaymentMethodService);
  private readonly checkoutState = inject(CheckoutStateService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly passosCheckout = [
    { label: 'Endereço' },
    { label: 'Frete' },
    { label: 'Pagamento' },
    { label: 'Revisão' },
  ];
  readonly abasPagamento: { valor: AbaPagamento; label: string }[] = [
    { valor: 'CARTAO', label: 'Cartão' },
    { valor: 'PIX', label: 'PIX' },
    { valor: 'BOLETO', label: 'Boleto' },
  ];
  readonly OPCOES_PARCELAS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

  readonly abaAtiva = signal<AbaPagamento>('CARTAO');
  readonly metodosCadastrados = signal<FormaPagamentoResponse[]>([]);
  readonly cartoesSalvos = computed(() =>
    this.metodosCadastrados().filter((metodoCadastrado) => metodoCadastrado.tipo === 'CARTAO_CREDITO'),
  );
  readonly cartaoSelecionadoId = signal<number | null>(null);
  readonly quantidadeParcelas = signal(1);
  readonly mostrandoFormularioCartao = signal(false);
  readonly tokenizandoCartao = signal(false);
  readonly erroCartao = signal<string | null>(null);

  readonly formularioCartao = this.formBuilder.nonNullable.group({
    numero: ['', [Validators.required, Validators.pattern(/^\d{13,19}$/)]],
    nomeImpresso: ['', [Validators.required, Validators.maxLength(100)]],
    validadeMes: ['', [Validators.required, Validators.pattern(/^(0?[1-9]|1[0-2])$/)]],
    validadeAno: ['', [Validators.required, Validators.pattern(/^20\d{2}$/)]],
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
  });

  readonly podeProsseguir = computed(() => {
    if (this.abaAtiva() === 'CARTAO') return this.cartaoSelecionadoId() !== null;
    return true;
  });

  ngOnInit(): void {
    const estadoSnapshot = this.checkoutState.state();
    this.quantidadeParcelas.set(estadoSnapshot.parcelas || 1);
    this.carregarMetodos(estadoSnapshot.formaPagamentoId);
  }

  trocarAba(novaAba: AbaPagamento): void {
    this.abaAtiva.set(novaAba);
  }

  selecionarCartao(idCartao: number): void {
    this.cartaoSelecionadoId.set(idCartao);
  }

  atualizarParcelas(valorBruto: string): void {
    const valorNumerico = Number(valorBruto);
    if (Number.isInteger(valorNumerico) && valorNumerico >= 1 && valorNumerico <= 12) {
      this.quantidadeParcelas.set(valorNumerico);
    }
  }

  tokenizarECadastrarCartao(): void {
    if (this.formularioCartao.invalid) {
      this.formularioCartao.markAllAsTouched();
      return;
    }
    const valoresCartao = this.formularioCartao.getRawValue();
    this.tokenizandoCartao.set(true);
    this.erroCartao.set(null);
    this.paymentMethodService.tokenize({
      numero: valoresCartao.numero.replace(/\D/g, ''),
      cvv: valoresCartao.cvv,
      nomeImpresso: valoresCartao.nomeImpresso,
      validadeMes: Number(valoresCartao.validadeMes),
      validadeAno: Number(valoresCartao.validadeAno),
    }).subscribe({
      next: (respostaTokenize) => {
        // Limpa campos sensíveis IMEDIATAMENTE após tokenizar — PAN/CVV
        // não devem ficar em memória além do necessário.
        this.formularioCartao.patchValue({ numero: '', cvv: '' });
        const requestCadastrarMetodo: CreateFormaPagamentoRequest = {
          tipo: 'CARTAO_CREDITO',
          gatewayToken: respostaTokenize.token,
          bandeira: respostaTokenize.bandeira,
          ultimosQuatroDigitos: respostaTokenize.ultimosQuatroDigitos,
          nomeImpresso: valoresCartao.nomeImpresso,
          validadeMes: Number(valoresCartao.validadeMes),
          validadeAno: Number(valoresCartao.validadeAno),
          padrao: this.cartoesSalvos().length === 0,
        };
        this.paymentMethodService.create(requestCadastrarMetodo).subscribe({
          next: (metodoCriado) => {
            this.tokenizandoCartao.set(false);
            this.metodosCadastrados.update((listaMetodos) => [...listaMetodos, metodoCriado]);
            this.cartaoSelecionadoId.set(metodoCriado.id);
            this.mostrandoFormularioCartao.set(false);
            this.formularioCartao.reset();
          },
          error: (httpError: HttpErrorResponse) => {
            this.tokenizandoCartao.set(false);
            this.erroCartao.set(httpError.error?.detail ?? 'Não foi possível salvar o cartão.');
          },
        });
      },
      error: (httpError: HttpErrorResponse) => {
        this.tokenizandoCartao.set(false);
        this.erroCartao.set(httpError.error?.detail ?? 'Número do cartão inválido.');
      },
    });
  }

  prosseguirParaRevisao(): void {
    if (this.abaAtiva() === 'CARTAO') {
      this.checkoutState.patch({
        formaPagamentoId: this.cartaoSelecionadoId(),
        parcelas: this.quantidadeParcelas(),
      });
      this.router.navigate(['/checkout/revisao']);
      return;
    }
    const tipoEscolhido: TipoPagamento = this.abaAtiva() === 'PIX' ? 'PIX' : 'BOLETO';
    const metodoExistente = this.metodosCadastrados().find((metodo) => metodo.tipo === tipoEscolhido);
    if (metodoExistente) {
      this.checkoutState.patch({ formaPagamentoId: metodoExistente.id, parcelas: 1 });
      this.router.navigate(['/checkout/revisao']);
      return;
    }
    this.paymentMethodService.create({ tipo: tipoEscolhido }).subscribe({
      next: (metodoCriado) => {
        this.metodosCadastrados.update((listaMetodos) => [...listaMetodos, metodoCriado]);
        this.checkoutState.patch({ formaPagamentoId: metodoCriado.id, parcelas: 1 });
        this.router.navigate(['/checkout/revisao']);
      },
      error: (httpError: HttpErrorResponse) => {
        this.erroCartao.set(httpError.error?.detail ?? 'Não foi possível registrar o método.');
      },
    });
  }

  voltarParaFrete(): void {
    this.router.navigate(['/checkout/frete']);
  }

  private carregarMetodos(idMetodoPreSelecionado: number | null): void {
    this.paymentMethodService.list().subscribe({
      next: (listaMetodos) => {
        this.metodosCadastrados.set(listaMetodos);
        if (idMetodoPreSelecionado) {
          const metodoPrevio = listaMetodos.find((metodo) => metodo.id === idMetodoPreSelecionado);
          if (metodoPrevio) {
            if (metodoPrevio.tipo === 'CARTAO_CREDITO') {
              this.abaAtiva.set('CARTAO');
              this.cartaoSelecionadoId.set(metodoPrevio.id);
            } else if (metodoPrevio.tipo === 'PIX') {
              this.abaAtiva.set('PIX');
            } else if (metodoPrevio.tipo === 'BOLETO') {
              this.abaAtiva.set('BOLETO');
            }
            return;
          }
        }
        const cartaoPadrao = listaMetodos.find((metodo) => metodo.tipo === 'CARTAO_CREDITO');
        if (cartaoPadrao) {
          this.cartaoSelecionadoId.set(cartaoPadrao.id);
        } else {
          this.mostrandoFormularioCartao.set(true);
        }
      },
      error: () => this.mostrandoFormularioCartao.set(true),
    });
  }
}

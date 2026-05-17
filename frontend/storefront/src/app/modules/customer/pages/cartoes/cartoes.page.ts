import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { PaymentMethodService } from '@modules/customer/services/payment-method.service';
import { FormaPagamentoResponse, CreateFormaPagamentoRequest } from '@modules/customer/models/payment-method';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';
import { ToastService } from '@shared/services/toast.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

@Component({
  selector: 'app-cartoes-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  templateUrl: './cartoes.page.html',
})
export class CartoesPage implements OnInit {
  private readonly pm = inject(PaymentMethodService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly carregando = signal(true);
  readonly erroCarregamento = signal(false);
  readonly cartoes = signal<FormaPagamentoResponse[]>([]);
  readonly mostrandoFormulario = signal(false);
  readonly salvando = signal(false);
  readonly anosValidade = Array.from({ length: 15 }, (_, indice) => new Date().getFullYear() + indice);
  readonly mesesValidade = Array.from({ length: 12 }, (_, indice) => indice + 1);

  readonly formularioCartao = this.fb.nonNullable.group({
    numero: ['', [Validators.required, Validators.pattern(/^\d{13,19}$/)]],
    nomeImpresso: ['', [Validators.required, Validators.maxLength(50)]],
    validadeMes: this.fb.nonNullable.control<number>(1, [Validators.required, Validators.min(1), Validators.max(12)]),
    validadeAno: this.fb.nonNullable.control<number>(new Date().getFullYear(), Validators.required),
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
    apelido: [''],
    padrao: [false],
  });

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    this.pm.list().subscribe({
      next: lista => { this.cartoes.set(lista); this.carregando.set(false); },
      error: () => { this.erroCarregamento.set(true); this.carregando.set(false); },
    });
  }

  iniciarAdicao(): void {
    this.mostrandoFormulario.set(true);
    this.formularioCartao.reset({
      numero: '', nomeImpresso: '', validadeMes: 1,
      validadeAno: new Date().getFullYear(), cvv: '', apelido: '', padrao: false,
    });
  }

  cancelarAdicao(): void { this.mostrandoFormulario.set(false); }

  salvar(): void {
    if (this.formularioCartao.invalid) return;
    this.salvando.set(true);
    const valor = this.formularioCartao.getRawValue();

    this.pm.tokenize({
      numero: valor.numero,
      cvv: valor.cvv,
      nomeImpresso: valor.nomeImpresso,
      validadeMes: valor.validadeMes,
      validadeAno: valor.validadeAno,
    }).subscribe({
      next: tokenResp => {
        this.formularioCartao.patchValue({ numero: '', cvv: '' });

        const requisicao: CreateFormaPagamentoRequest = {
          tipo: 'CARTAO_CREDITO',
          apelido: valor.apelido || undefined,
          gatewayToken: tokenResp.token,
          bandeira: tokenResp.bandeira,
          ultimosQuatroDigitos: tokenResp.ultimosQuatroDigitos,
          nomeImpresso: valor.nomeImpresso,
          validadeMes: valor.validadeMes,
          validadeAno: valor.validadeAno,
          padrao: valor.padrao,
        };
        this.pm.create(requisicao).subscribe({
          next: () => {
            this.salvando.set(false);
            this.mostrandoFormulario.set(false);
            this.toast.success('Cartão cadastrado.');
            this.carregar();
          },
          error: (erro: HttpErrorResponse) => {
            this.salvando.set(false);
            this.toast.error(erro.error?.detail ?? 'Não foi possível salvar cartão.');
          },
        });
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        this.toast.error(erro.error?.detail ?? 'Não foi possível tokenizar cartão.');
      },
    });
  }

  tornarPadrao(cartao: FormaPagamentoResponse): void {
    this.pm.setDefault(cartao.id).subscribe({
      next: () => { this.toast.success('Cartão definido como padrão.'); this.carregar(); },
      error: () => this.toast.error('Não foi possível definir como padrão.'),
    });
  }

  async remover(cartao: FormaPagamentoResponse): Promise<void> {
    const confirmado = await this.confirmDialog.open({
      titulo: 'Remover cartão?',
      mensagem: `Remover cartão ${cartao.bandeira ?? cartao.tipo} •••• ${cartao.ultimosQuatroDigitos ?? '----'}?`,
      acaoLabel: 'Remover',
      acaoVariant: 'danger',
    });
    if (!confirmado) return;

    this.pm.remove(cartao.id).subscribe({
      next: () => { this.toast.success('Cartão removido.'); this.carregar(); },
      error: () => this.toast.error('Não foi possível remover.'),
    });
  }
}

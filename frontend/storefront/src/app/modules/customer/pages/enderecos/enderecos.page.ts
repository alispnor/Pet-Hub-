import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AddressService } from '@modules/customer/services/address.service';
import {
  EnderecoResponse,
  TipoEndereco,
  UFS,
  UnidadeFederativa,
  CreateEnderecoRequest,
  UpdateEnderecoRequest,
} from '@modules/customer/models/address';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';
import { ToastService } from '@shared/services/toast.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

@Component({
  selector: 'app-enderecos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  templateUrl: './enderecos.page.html',
})
export class EnderecosPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly LISTA_UFS = UFS;
  readonly tiposEndereco: TipoEndereco[] = ['RESIDENCIAL', 'COMERCIAL'];

  readonly carregando = signal(true);
  readonly erroCarregamento = signal(false);
  readonly enderecos = signal<EnderecoResponse[]>([]);
  readonly idEmEdicao = signal<number | null>(null);
  readonly salvando = signal(false);
  readonly buscandoCep = signal(false);

  readonly formulario = this.fb.nonNullable.group({
    apelido: ['', [Validators.required, Validators.maxLength(50)]],
    cep: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    logradouro: ['', [Validators.required, Validators.maxLength(200)]],
    numero: [''],
    complemento: [''],
    bairro: ['', [Validators.required, Validators.maxLength(100)]],
    cidade: ['', [Validators.required, Validators.maxLength(100)]],
    uf: this.fb.nonNullable.control<UnidadeFederativa>('SP', Validators.required),
    tipo: this.fb.nonNullable.control<TipoEndereco>('RESIDENCIAL', Validators.required),
    padraoEntrega: [false],
    padraoCobranca: [false],
  });

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    this.addressService.list().subscribe({
      next: lista => { this.enderecos.set(lista); this.carregando.set(false); },
      error: () => { this.erroCarregamento.set(true); this.carregando.set(false); },
    });
  }

  iniciarNovo(): void {
    this.idEmEdicao.set(-1);
    this.formulario.reset({
      apelido: '', cep: '', logradouro: '', numero: '', complemento: '',
      bairro: '', cidade: '', uf: 'SP', tipo: 'RESIDENCIAL',
      padraoEntrega: false, padraoCobranca: false,
    });
  }

  iniciarEdicao(endereco: EnderecoResponse): void {
    this.idEmEdicao.set(endereco.id);
    this.formulario.reset({
      apelido: endereco.apelido,
      cep: endereco.cep,
      logradouro: endereco.logradouro,
      numero: endereco.numero ?? '',
      complemento: endereco.complemento ?? '',
      bairro: endereco.bairro,
      cidade: endereco.cidade,
      uf: endereco.uf,
      tipo: endereco.tipo,
      padraoEntrega: endereco.padraoEntrega,
      padraoCobranca: endereco.padraoCobranca,
    });
  }

  cancelarEdicao(): void { this.idEmEdicao.set(null); }

  buscarCep(): void {
    const valorCep = (this.formulario.controls.cep.value || '').replace(/\D/g, '');
    if (valorCep.length !== 8) return;
    this.buscandoCep.set(true);
    this.addressService.lookupCep(valorCep).subscribe({
      next: viaCep => {
        if (viaCep.erro) {
          this.toast.error('CEP não encontrado.');
        } else {
          this.formulario.patchValue({
            logradouro: viaCep.logradouro,
            bairro: viaCep.bairro,
            cidade: viaCep.cidade,
            uf: viaCep.uf as UnidadeFederativa,
          });
        }
        this.buscandoCep.set(false);
      },
      error: () => {
        this.toast.error('Erro ao consultar CEP.');
        this.buscandoCep.set(false);
      },
    });
  }

  salvar(): void {
    if (this.formulario.invalid) return;
    this.salvando.set(true);
    const valor = this.formulario.getRawValue();
    const idAtual = this.idEmEdicao();

    if (idAtual === -1) {
      const requisicao: CreateEnderecoRequest = {
        apelido: valor.apelido,
        cep: valor.cep,
        logradouro: valor.logradouro,
        numero: valor.numero || undefined,
        complemento: valor.complemento || undefined,
        bairro: valor.bairro,
        cidade: valor.cidade,
        uf: valor.uf,
        tipo: valor.tipo,
        padraoEntrega: valor.padraoEntrega,
        padraoCobranca: valor.padraoCobranca,
      };
      this.addressService.create(requisicao).subscribe({
        next: () => this.aoSalvarComSucesso('Endereço cadastrado.'),
        error: (erro: HttpErrorResponse) => this.aoFalharSalvar(erro),
      });
    } else if (idAtual !== null) {
      const requisicao: UpdateEnderecoRequest = {
        apelido: valor.apelido,
        cep: valor.cep,
        logradouro: valor.logradouro,
        numero: valor.numero,
        complemento: valor.complemento,
        bairro: valor.bairro,
        cidade: valor.cidade,
        uf: valor.uf,
        tipo: valor.tipo,
      };
      this.addressService.update(idAtual, requisicao).subscribe({
        next: () => this.aoSalvarComSucesso('Endereço atualizado.'),
        error: (erro: HttpErrorResponse) => this.aoFalharSalvar(erro),
      });
    }
  }

  private aoSalvarComSucesso(mensagem: string): void {
    this.salvando.set(false);
    this.idEmEdicao.set(null);
    this.toast.success(mensagem);
    this.carregar();
  }

  private aoFalharSalvar(erro: HttpErrorResponse): void {
    this.salvando.set(false);
    this.toast.error(erro.error?.detail ?? 'Não foi possível salvar.');
  }

  definirComoPadrao(endereco: EnderecoResponse, tipoPadrao: 'entrega' | 'cobranca'): void {
    this.addressService.setDefault(endereco.id, tipoPadrao === 'entrega'
      ? { padraoEntrega: true }
      : { padraoCobranca: true }
    ).subscribe({
      next: () => {
        this.toast.success(`Definido como padrão de ${tipoPadrao}.`);
        this.carregar();
      },
      error: () => this.toast.error('Não foi possível definir como padrão.'),
    });
  }

  async remover(endereco: EnderecoResponse): Promise<void> {
    const confirmado = await this.confirmDialog.open({
      titulo: 'Remover endereço?',
      mensagem: `Remover "${endereco.apelido}"? Esta ação não pode ser desfeita.`,
      acaoLabel: 'Remover',
      acaoVariant: 'danger',
    });
    if (!confirmado) return;

    this.addressService.remove(endereco.id).subscribe({
      next: () => {
        this.toast.success('Endereço removido.');
        this.carregar();
      },
      error: () => this.toast.error('Não foi possível remover.'),
    });
  }
}

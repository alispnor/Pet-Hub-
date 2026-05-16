import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AddressService } from '@modules/customer/services/address.service';
import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { EnderecoResponse, TipoEndereco, UFS, UnidadeFederativa } from '@modules/customer/models/address';
import { StepperComponent } from '@modules/checkout/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-address-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StepperComponent],
  templateUrl: './address.page.html',
  styleUrls: ['./address.page.scss'],
})
export class CheckoutAddressPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly checkoutState = inject(CheckoutStateService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly LISTA_UFS = UFS;
  readonly passosCheckout = [
    { label: 'Endereço' },
    { label: 'Frete' },
    { label: 'Pagamento' },
    { label: 'Revisão' },
  ];

  readonly enderecosCadastrados = signal<EnderecoResponse[]>([]);
  readonly enderecoEntregaId = signal<number | null>(null);
  readonly enderecoCobrancaId = signal<number | null>(null);
  readonly usarMesmoEnderecoCobranca = signal(true);
  readonly mostrandoFormularioNovo = signal(false);
  readonly salvandoEndereco = signal(false);
  readonly erroCep = signal<string | null>(null);
  readonly erroFormulario = signal<string | null>(null);

  readonly formularioEndereco = this.formBuilder.nonNullable.group({
    apelido: ['', [Validators.required, Validators.maxLength(50)]],
    cep: ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
    logradouro: ['', [Validators.required, Validators.maxLength(200)]],
    numero: [''],
    complemento: [''],
    bairro: ['', [Validators.required, Validators.maxLength(100)]],
    cidade: ['', [Validators.required, Validators.maxLength(100)]],
    uf: this.formBuilder.nonNullable.control<UnidadeFederativa>('SP', Validators.required),
    tipo: this.formBuilder.nonNullable.control<TipoEndereco>('RESIDENCIAL', Validators.required),
  });

  ngOnInit(): void {
    const estadoInicial = this.checkoutState.state();
    if (estadoInicial.enderecoEntregaId) this.enderecoEntregaId.set(estadoInicial.enderecoEntregaId);
    if (estadoInicial.enderecoCobrancaId) this.enderecoCobrancaId.set(estadoInicial.enderecoCobrancaId);
    if (estadoInicial.enderecoEntregaId && estadoInicial.enderecoCobrancaId
        && estadoInicial.enderecoEntregaId !== estadoInicial.enderecoCobrancaId) {
      this.usarMesmoEnderecoCobranca.set(false);
    }

    this.addressService.list().subscribe({
      next: (listaEnderecos) => {
        this.enderecosCadastrados.set(listaEnderecos);
        if (!this.enderecoEntregaId()) {
          const enderecoPadrao = listaEnderecos.find((endereco) => endereco.padraoEntrega) ?? listaEnderecos[0];
          if (enderecoPadrao) {
            this.enderecoEntregaId.set(enderecoPadrao.id);
            this.enderecoCobrancaId.set(enderecoPadrao.id);
          } else {
            this.mostrandoFormularioNovo.set(true);
          }
        }
      },
      error: () => this.mostrandoFormularioNovo.set(true),
    });
  }

  selecionarEnderecoEntrega(idEndereco: number): void {
    this.enderecoEntregaId.set(idEndereco);
    if (this.usarMesmoEnderecoCobranca()) this.enderecoCobrancaId.set(idEndereco);
  }

  selecionarEnderecoCobranca(idEndereco: number): void {
    this.enderecoCobrancaId.set(idEndereco);
  }

  alternarMesmoEnderecoCobranca(): void {
    const proximoValor = !this.usarMesmoEnderecoCobranca();
    this.usarMesmoEnderecoCobranca.set(proximoValor);
    if (proximoValor) this.enderecoCobrancaId.set(this.enderecoEntregaId());
  }

  autopreencherViaCep(): void {
    const cepSemMascara = this.formularioEndereco.controls.cep.value.replace(/\D/g, '');
    if (cepSemMascara.length !== 8) return;
    this.erroCep.set(null);
    this.addressService.lookupCep(cepSemMascara).subscribe({
      next: (respostaCep) => {
        if (respostaCep.erro) {
          this.erroCep.set('CEP não encontrado. Preencha manualmente.');
          return;
        }
        this.formularioEndereco.patchValue({
          logradouro: respostaCep.logradouro || this.formularioEndereco.controls.logradouro.value,
          bairro: respostaCep.bairro || this.formularioEndereco.controls.bairro.value,
          cidade: respostaCep.cidade || this.formularioEndereco.controls.cidade.value,
          uf: (respostaCep.uf as UnidadeFederativa) || this.formularioEndereco.controls.uf.value,
        });
      },
      error: () => this.erroCep.set('CEP não encontrado. Preencha manualmente.'),
    });
  }

  salvarNovoEndereco(): void {
    if (this.formularioEndereco.invalid) {
      this.formularioEndereco.markAllAsTouched();
      return;
    }
    this.salvandoEndereco.set(true);
    this.erroFormulario.set(null);
    const valoresFormulario = this.formularioEndereco.getRawValue();
    this.addressService.create({
      ...valoresFormulario,
      cep: valoresFormulario.cep.replace(/\D/g, ''),
      padraoEntrega: this.enderecosCadastrados().length === 0,
      padraoCobranca: this.enderecosCadastrados().length === 0,
    }).subscribe({
      next: (enderecoCriado) => {
        this.salvandoEndereco.set(false);
        this.enderecosCadastrados.update((lista) => [...lista, enderecoCriado]);
        this.enderecoEntregaId.set(enderecoCriado.id);
        if (this.usarMesmoEnderecoCobranca()) this.enderecoCobrancaId.set(enderecoCriado.id);
        this.mostrandoFormularioNovo.set(false);
        this.formularioEndereco.reset({
          uf: 'SP', tipo: 'RESIDENCIAL',
          apelido: '', cep: '', logradouro: '', numero: '',
          complemento: '', bairro: '', cidade: '',
        });
      },
      error: (httpError: HttpErrorResponse) => {
        this.salvandoEndereco.set(false);
        this.erroFormulario.set(httpError.error?.detail ?? 'Não foi possível salvar o endereço.');
      },
    });
  }

  prosseguirParaFrete(): void {
    this.checkoutState.patch({
      enderecoEntregaId: this.enderecoEntregaId(),
      enderecoCobrancaId: this.enderecoCobrancaId(),
    });
    this.router.navigate(['/checkout/frete']);
  }

  formatarCep(cep: string): string {
    const digitos = cep.replace(/\D/g, '');
    return digitos.length === 8 ? `${digitos.slice(0, 5)}-${digitos.slice(5)}` : cep;
  }
}

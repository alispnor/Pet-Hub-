import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { PetService } from '@modules/customer/services/pet.service';
import {
  PetResponse,
  Especie,
  Porte,
  ESPECIES_LABEL,
  PORTES_LABEL,
  CreatePetRequest,
  UpdatePetRequest,
} from '@modules/customer/models/pet';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';
import { ToastService } from '@shared/services/toast.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

const TAMANHO_MAX_FOTO_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-pets-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  templateUrl: './pets.page.html',
})
export class PetsPage implements OnInit {
  private readonly petService = inject(PetService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly ESPECIES_LABEL = ESPECIES_LABEL;
  readonly PORTES_LABEL = PORTES_LABEL;
  readonly listaEspecies: Especie[] = ['CACHORRO', 'GATO', 'AVE', 'PEIXE', 'REPTIL', 'OUTROS'];
  readonly listaPortes: Porte[] = ['PEQUENO', 'MEDIO', 'GRANDE', 'GIGANTE'];

  readonly carregando = signal(true);
  readonly erroCarregamento = signal(false);
  readonly pets = signal<PetResponse[]>([]);
  readonly idEmEdicao = signal<number | null>(null);
  readonly salvando = signal(false);
  readonly arquivoFoto = signal<File | null>(null);
  readonly previewFotoUrl = signal<string | null>(null);

  readonly formulario = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    especie: this.fb.nonNullable.control<Especie>('CACHORRO', Validators.required),
    raca: [''],
    dataNascimento: [''],
    pesoKg: this.fb.nonNullable.control<number | null>(null),
    porte: this.fb.nonNullable.control<Porte | ''>(''),
    observacoes: [''],
  });

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    this.petService.list().subscribe({
      next: lista => { this.pets.set(lista); this.carregando.set(false); },
      error: () => { this.erroCarregamento.set(true); this.carregando.set(false); },
    });
  }

  iniciarNovo(): void {
    this.idEmEdicao.set(-1);
    this.formulario.reset({
      nome: '', especie: 'CACHORRO', raca: '', dataNascimento: '',
      pesoKg: null, porte: '', observacoes: '',
    });
    this.arquivoFoto.set(null);
    this.previewFotoUrl.set(null);
  }

  iniciarEdicao(pet: PetResponse): void {
    this.idEmEdicao.set(pet.id);
    this.formulario.reset({
      nome: pet.nome,
      especie: pet.especie,
      raca: pet.raca ?? '',
      dataNascimento: pet.dataNascimento ?? '',
      pesoKg: pet.pesoKg,
      porte: pet.porte ?? '',
      observacoes: pet.observacoes ?? '',
    });
    this.arquivoFoto.set(null);
    this.previewFotoUrl.set(null);
  }

  cancelarEdicao(): void {
    this.idEmEdicao.set(null);
    this.previewFotoUrl.set(null);
  }

  aoSelecionarArquivo(eventoInput: Event): void {
    const input = eventoInput.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;
    if (!arquivo) {
      this.arquivoFoto.set(null);
      this.previewFotoUrl.set(null);
      return;
    }
    if (arquivo.size > TAMANHO_MAX_FOTO_BYTES) {
      this.toast.error('Foto não pode passar de 5 MB.');
      input.value = '';
      return;
    }
    this.arquivoFoto.set(arquivo);
    this.previewFotoUrl.set(URL.createObjectURL(arquivo));
  }

  salvar(): void {
    if (this.formulario.invalid) return;
    this.salvando.set(true);
    const valor = this.formulario.getRawValue();
    const idAtual = this.idEmEdicao();

    const dadosBase = {
      nome: valor.nome,
      especie: valor.especie,
      raca: valor.raca || undefined,
      dataNascimento: valor.dataNascimento || undefined,
      pesoKg: valor.pesoKg ?? undefined,
      porte: valor.porte === '' ? undefined : valor.porte as Porte,
      observacoes: valor.observacoes || undefined,
    };

    if (idAtual === -1) {
      const requisicao: CreatePetRequest = dadosBase as CreatePetRequest;
      this.petService.create(requisicao).subscribe({
        next: petCriado => this.aoSalvarPetSuccesso(petCriado, 'Pet cadastrado.'),
        error: (erro: HttpErrorResponse) => this.aoFalharSalvar(erro),
      });
    } else if (idAtual !== null) {
      const requisicao: UpdatePetRequest = dadosBase;
      this.petService.update(idAtual, requisicao).subscribe({
        next: petAtualizado => this.aoSalvarPetSuccesso(petAtualizado, 'Pet atualizado.'),
        error: (erro: HttpErrorResponse) => this.aoFalharSalvar(erro),
      });
    }
  }

  private aoSalvarPetSuccesso(pet: PetResponse, mensagem: string): void {
    const arquivo = this.arquivoFoto();
    if (arquivo) {
      this.petService.uploadPhoto(pet.id, arquivo).subscribe({
        next: () => this.finalizarSalvar(mensagem),
        error: () => {
          this.toast.error('Pet salvo, mas não foi possível enviar a foto.');
          this.finalizarSalvar(mensagem);
        },
      });
    } else {
      this.finalizarSalvar(mensagem);
    }
  }

  private finalizarSalvar(mensagem: string): void {
    this.salvando.set(false);
    this.idEmEdicao.set(null);
    this.arquivoFoto.set(null);
    this.previewFotoUrl.set(null);
    this.toast.success(mensagem);
    this.carregar();
  }

  private aoFalharSalvar(erro: HttpErrorResponse): void {
    this.salvando.set(false);
    this.toast.error(erro.error?.detail ?? 'Não foi possível salvar.');
  }

  async remover(pet: PetResponse): Promise<void> {
    const confirmado = await this.confirmDialog.open({
      titulo: 'Remover pet?',
      mensagem: `Remover "${pet.nome}"? Esta ação não pode ser desfeita.`,
      acaoLabel: 'Remover',
      acaoVariant: 'danger',
    });
    if (!confirmado) return;

    this.petService.remove(pet.id).subscribe({
      next: () => { this.toast.success('Pet removido.'); this.carregar(); },
      error: () => this.toast.error('Não foi possível remover.'),
    });
  }

  calcularIdade(dataNascimentoIso: string | null): string {
    if (!dataNascimentoIso) return '';
    const nascimento = new Date(dataNascimentoIso);
    const agora = new Date();
    let anos = agora.getFullYear() - nascimento.getFullYear();
    const m = agora.getMonth() - nascimento.getMonth();
    if (m < 0 || (m === 0 && agora.getDate() < nascimento.getDate())) anos--;
    if (anos <= 0) return 'menos de 1 ano';
    return anos === 1 ? '1 ano' : `${anos} anos`;
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { PerfilService } from '@modules/customer/services/perfil.service';
import { PerfilResponse, Genero, GENEROS_LABEL, UpdatePerfilRequest } from '@modules/customer/models/perfil';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';
import { ToastService } from '@shared/services/toast.service';

@Component({
  selector: 'app-perfil-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  templateUrl: './perfil.page.html',
})
export class PerfilPage implements OnInit {
  private readonly perfilService = inject(PerfilService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly GENEROS_LABEL = GENEROS_LABEL;
  readonly listaGeneros: Genero[] = ['MASCULINO', 'FEMININO', 'NAO_INFORMADO', 'OUTRO'];

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erroCarregamento = signal(false);
  readonly perfilAtual = signal<PerfilResponse | null>(null);

  readonly formulario = this.fb.nonNullable.group({
    telefoneAdicional: ['', [Validators.pattern(/^\d{10,11}$/)]],
    dataNascimento: [''],
    genero: this.fb.nonNullable.control<Genero | ''>(''),
    aceiteTermos: [false],
    aceiteMarketing: [false],
  });

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    this.perfilService.get().subscribe({
      next: dados => {
        this.perfilAtual.set(dados);
        this.formulario.reset({
          telefoneAdicional: dados.telefoneAdicional ?? '',
          dataNascimento: dados.dataNascimento ?? '',
          genero: dados.genero ?? '',
          aceiteTermos: dados.aceiteTermos,
          aceiteMarketing: dados.aceiteMarketing,
        });
        this.carregando.set(false);
      },
      error: () => {
        this.erroCarregamento.set(true);
        this.carregando.set(false);
      },
    });
  }

  salvar(): void {
    if (this.formulario.invalid || !this.formulario.dirty) return;
    this.salvando.set(true);
    const valor = this.formulario.getRawValue();
    const requisicao: UpdatePerfilRequest = {
      telefoneAdicional: valor.telefoneAdicional || null,
      dataNascimento: valor.dataNascimento || null,
      genero: valor.genero === '' ? null : valor.genero as Genero,
      aceiteTermos: valor.aceiteTermos,
      aceiteMarketing: valor.aceiteMarketing,
    };
    this.perfilService.update(requisicao).subscribe({
      next: atualizado => {
        this.perfilAtual.set(atualizado);
        this.formulario.markAsPristine();
        this.salvando.set(false);
        this.toast.success('Perfil atualizado com sucesso.');
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.detail ?? 'Não foi possível salvar.';
        this.toast.error(mensagem);
      },
    });
  }
}

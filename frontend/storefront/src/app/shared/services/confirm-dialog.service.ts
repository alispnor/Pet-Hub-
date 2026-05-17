import { Injectable, signal } from '@angular/core';

export type VarianteAcao = 'primary' | 'danger';

export interface ConfirmacaoPendente {
  titulo: string;
  mensagem: string;
  acaoLabel: string;
  acaoVariant: VarianteAcao;
  resolve: (confirmado: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  readonly pendente = signal<ConfirmacaoPendente | null>(null);

  open(opcoes: {
    titulo: string;
    mensagem: string;
    acaoLabel: string;
    acaoVariant?: VarianteAcao;
  }): Promise<boolean> {
    return new Promise<boolean>(resolve => {
      this.pendente.set({
        titulo: opcoes.titulo,
        mensagem: opcoes.mensagem,
        acaoLabel: opcoes.acaoLabel,
        acaoVariant: opcoes.acaoVariant ?? 'primary',
        resolve,
      });
    });
  }

  responder(confirmado: boolean): void {
    const atual = this.pendente();
    if (atual) {
      atual.resolve(confirmado);
      this.pendente.set(null);
    }
  }
}

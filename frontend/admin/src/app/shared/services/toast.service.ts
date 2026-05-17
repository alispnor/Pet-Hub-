import { Injectable, signal } from '@angular/core';

export type TipoToast = 'success' | 'error' | 'info';

export interface Toast {
  id: string;
  tipo: TipoToast;
  mensagem: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly mensagens = signal<Toast[]>([]);

  success(mensagem: string): void { this.empilhar('success', mensagem); }
  error(mensagem: string): void { this.empilhar('error', mensagem); }
  info(mensagem: string): void { this.empilhar('info', mensagem); }

  dismiss(id: string): void {
    this.mensagens.update(lista => lista.filter(toast => toast.id !== id));
  }

  private empilhar(tipo: TipoToast, mensagem: string): void {
    const id = crypto.randomUUID();
    this.mensagens.update(lista => [...lista, { id, tipo, mensagem }]);
    setTimeout(() => this.dismiss(id), 5000);
  }
}

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '@shared/services/toast.service';

@Component({
  selector: 'app-toast-stack',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm" aria-live="polite" aria-atomic="false">
      <div *ngFor="let toast of toastService.mensagens()"
           [class]="classesPorTipo(toast)"
           class="rounded-md border px-4 py-3 shadow-md flex items-start gap-3 animate-fade-in">
        <span class="text-sm flex-1">{{ toast.mensagem }}</span>
        <button type="button" (click)="toastService.dismiss(toast.id)"
                class="text-sm opacity-70 hover:opacity-100" aria-label="Fechar notificação">✕</button>
      </div>
    </div>
  `,
  styles: [`
    @keyframes fade-in { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: none; } }
    .animate-fade-in { animation: fade-in 150ms ease-out; }
  `],
})
export class ToastStackComponent {
  readonly toastService = inject(ToastService);

  classesPorTipo(toast: Toast): string {
    switch (toast.tipo) {
      case 'success': return 'bg-emerald-50 border-emerald-200 text-emerald-900';
      case 'error':   return 'bg-red-50 border-red-200 text-red-900';
      default:        return 'bg-blue-50 border-blue-200 text-blue-900';
    }
  }
}

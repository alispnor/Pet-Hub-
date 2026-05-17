import { AfterViewInit, Component, ElementRef, ViewChild, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <dialog #dialogo class="rounded-lg p-0 backdrop:bg-graphite-900/40 max-w-md w-full"
            (close)="aoFechar()" (cancel)="$event.preventDefault(); cancelar()">
      <ng-container *ngIf="service.pendente() as pendente">
        <div class="p-6">
          <h2 class="text-lg font-semibold text-graphite-900">{{ pendente.titulo }}</h2>
          <p class="mt-2 text-sm text-graphite-700">{{ pendente.mensagem }}</p>
          <div class="mt-6 flex justify-end gap-3">
            <button type="button" class="btn-ghost text-sm" (click)="cancelar()">Cancelar</button>
            <button type="button"
                    [class]="pendente.acaoVariant === 'danger' ? 'btn-danger' : 'btn-primary'"
                    class="text-sm" (click)="confirmar()">
              {{ pendente.acaoLabel }}
            </button>
          </div>
        </div>
      </ng-container>
    </dialog>
  `,
  styles: [`
    dialog[open] { display: block; }
    .btn-danger { background-color: rgb(220 38 38); color: white; padding: 0.5rem 1rem; border-radius: 0.375rem; }
    .btn-danger:hover { background-color: rgb(185 28 28); }
  `],
})
export class ConfirmDialogComponent implements AfterViewInit {
  @ViewChild('dialogo') dialogoRef!: ElementRef<HTMLDialogElement>;
  readonly service = inject(ConfirmDialogService);

  constructor() {
    effect(() => {
      const pendente = this.service.pendente();
      const dialogo = this.dialogoRef?.nativeElement;
      if (!dialogo) return;
      if (pendente && !dialogo.open) {
        dialogo.showModal();
      } else if (!pendente && dialogo.open) {
        dialogo.close();
      }
    });
  }

  ngAfterViewInit(): void { /* effect roda após inicializar */ }

  confirmar(): void { this.service.responder(true); }
  cancelar(): void { this.service.responder(false); }

  aoFechar(): void {
    if (this.service.pendente()) {
      this.service.responder(false);
    }
  }
}

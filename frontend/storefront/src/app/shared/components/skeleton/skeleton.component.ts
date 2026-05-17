import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

type Variante = 'text' | 'card' | 'avatar';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [class]="containerClasses" role="status" aria-live="polite" aria-busy="true">
      <ng-container [ngSwitch]="variant">
        <ng-container *ngSwitchCase="'avatar'">
          <div class="h-12 w-12 rounded-full bg-graphite-200 animate-pulse"></div>
        </ng-container>
        <ng-container *ngSwitchCase="'card'">
          <div class="h-32 w-full rounded-lg bg-graphite-200 animate-pulse"></div>
        </ng-container>
        <ng-container *ngSwitchDefault>
          <div *ngFor="let _ of repeticoes" class="h-3 w-full rounded bg-graphite-200 animate-pulse"></div>
        </ng-container>
      </ng-container>
      <span class="sr-only">Carregando...</span>
    </div>
  `,
})
export class SkeletonComponent {
  @Input() lines = 3;
  @Input() variant: Variante = 'text';

  get repeticoes(): number[] {
    return Array.from({ length: this.lines }, (_, indice) => indice);
  }

  get containerClasses(): string {
    return this.variant === 'text' ? 'space-y-2' : '';
  }
}

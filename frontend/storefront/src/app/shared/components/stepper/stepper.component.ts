import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Step {
  label: string;
  /** Rota correspondente (opcional — usado se um passo concluído for clicável no futuro). */
  route?: string;
}

/**
 * Barra visual de progresso 1•2•3•4 do checkout.
 *
 *  active = 1-based index do passo atual.
 *  Passos antes do active ficam check; o active fica destacado; depois ficam mutados.
 */
@Component({
  selector: 'app-stepper',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ol class="flex w-full items-center gap-2 sm:gap-4">
      <li *ngFor="let step of steps(); let i = index"
          class="flex flex-1 items-center gap-2">
        <span class="flex h-7 w-7 flex-shrink-0 items-center justify-center
                     rounded-full text-xs font-semibold"
              [class.bg-coral-500]="i + 1 === active()"
              [class.text-white]="i + 1 === active()"
              [class.bg-graphite-900]="i + 1 < active()"
              [class.bg-graphite-200]="i + 1 > active()"
              [class.text-graphite-500]="i + 1 > active()">
          <ng-container *ngIf="i + 1 < active(); else numeric">✓</ng-container>
          <ng-template #numeric>{{ i + 1 }}</ng-template>
        </span>
        <span class="hidden text-xs sm:inline"
              [class.text-graphite-900]="i + 1 === active()"
              [class.font-medium]="i + 1 === active()"
              [class.text-graphite-500]="i + 1 !== active()">
          {{ step.label }}
        </span>
        <span *ngIf="i + 1 < steps().length"
              class="flex-1 h-px"
              [class.bg-graphite-900]="i + 1 < active()"
              [class.bg-graphite-200]="i + 1 >= active()"></span>
      </li>
    </ol>
  `,
})
export class StepperComponent {
  steps = input.required<Step[]>();
  active = input.required<number>();
}

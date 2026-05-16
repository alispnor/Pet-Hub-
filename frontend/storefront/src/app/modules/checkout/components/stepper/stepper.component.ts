import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

interface PassoCheckout {
  label: string;
  /** Rota correspondente (opcional — usado se um passo concluído for clicável no futuro). */
  route?: string;
}

/**
 * Barra visual de progresso 1•2•3•4 do checkout.
 *
 *  active = 1-based index do passo atual.
 *  Passos antes do active ficam com check; o active fica destacado; depois mutados.
 *
 * As classes utility .stepper / .stepper__dot / .stepper__label / .stepper__line
 * com modifiers .is-active / .is-done estão em src/styles.css (camada components).
 */
@Component({
  selector: 'app-stepper',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stepper.component.html',
})
export class StepperComponent {
  steps = input.required<PassoCheckout[]>();
  active = input.required<number>();
}

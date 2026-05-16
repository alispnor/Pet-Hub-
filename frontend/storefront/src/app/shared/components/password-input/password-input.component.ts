import { Component, forwardRef, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Input de senha reutilizável com toggle de visualização (👁 / 👁‍🗨).
 *
 * Implementa {@link ControlValueAccessor} — pode ser usado com
 * `formControlName="senha"` exatamente como um `<input>` nativo:
 *
 * ```html
 * <app-password-input formControlName="senha"
 *                     [autocomplete]="'current-password'"
 *                     placeholder="••••••••" />
 * ```
 *
 * AppSec:
 * - O valor digitado vive APENAS dentro do controle Reactive Forms do parent.
 *   O componente não persiste em storage local.
 * - O toggle altera só o `type` do input HTML — o valor não é re-renderizado
 *   nem refletido em outro lugar.
 * - `autocomplete` é passado pelo parent (`current-password`, `new-password`)
 *   pra que o gerenciador do navegador faça o que faria com qualquer input
 *   de senha nativo.
 */
@Component({
  selector: 'app-password-input',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './password-input.component.html',
  styleUrls: ['./password-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PasswordInputComponent),
      multi: true,
    },
  ],
})
export class PasswordInputComponent implements ControlValueAccessor {
  inputId = input<string>('senha');
  autocomplete = input<string>('current-password');
  placeholder = input<string>('••••••••');
  /** Marca o input com `is-invalid` (usado pelo parent ao detectar erro de form). */
  invalido = input<boolean>(false);

  readonly senhaVisivel = signal(false);
  readonly valorAtual = signal<string>('');
  readonly desabilitado = signal(false);

  private onChangeHandler: (valor: string) => void = () => undefined;
  private onTouchedFn: () => void = () => undefined;

  alternarVisibilidade(): void {
    this.senhaVisivel.update((valorAnterior) => !valorAnterior);
  }

  onInputChange(eventoInput: Event): void {
    const valorNovo = (eventoInput.target as HTMLInputElement).value;
    this.valorAtual.set(valorNovo);
    this.onChangeHandler(valorNovo);
  }

  onTouchedHandler(): void {
    this.onTouchedFn();
  }

  // ---- ControlValueAccessor ----

  writeValue(valorRecebido: string | null): void {
    this.valorAtual.set(valorRecebido ?? '');
  }

  registerOnChange(callbackOnChange: (valor: string) => void): void {
    this.onChangeHandler = callbackOnChange;
  }

  registerOnTouched(callbackOnTouched: () => void): void {
    this.onTouchedFn = callbackOnTouched;
  }

  setDisabledState(estaDesabilitado: boolean): void {
    this.desabilitado.set(estaDesabilitado);
  }
}

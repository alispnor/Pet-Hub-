import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Exibe o preço em BRL com opção de parcelamento sugerido (1 a 12x sem juros
 * em compras acima de R$ 200, alinhado ao MockPaymentGateway).
 */
@Component({
  selector: 'app-price-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './price-display.component.html',
})
export class PriceDisplayComponent {
  valor = input.required<number>();
  /** Máximo de parcelas a sugerir; default = 12. */
  maxParcelas = input(12);
  /** Piso para começar a parcelar (sem juros). Default R$ 200. */
  minPraParcelar = input(200);

  protected parcelamentoSugerido = computed(() => {
    const valorAtual = this.valor();
    if (valorAtual < this.minPraParcelar()) return null;
    // Heurística: 1x até R$200 / 3x até R$600 / 6x até R$1500 / 12x acima
    const quantidadeParcelas = valorAtual < 600
      ? 3
      : valorAtual < 1500
        ? 6
        : Math.min(12, this.maxParcelas());
    const valorParcela = Math.round((valorAtual / quantidadeParcelas) * 100) / 100;
    return { quantidadeParcelas, valorParcela };
  });
}

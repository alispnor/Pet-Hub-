import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { ProdutoSummary } from '@modules/catalog/models/catalog';
import { PriceDisplayComponent } from '../price-display/price-display.component';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterLink, PriceDisplayComponent],
  templateUrl: './product-card.component.html',
})
export class ProductCardComponent {
  produto = input.required<ProdutoSummary>();
}

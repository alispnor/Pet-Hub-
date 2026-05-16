import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { CheckoutStateService } from '../services/checkout-state.service';
import { CartService } from '../services/cart.service';
import { CheckoutState } from '../models/checkout';

/**
 * Cada chave precisa estar populada no CheckoutState para o passo correspondente.
 *
 *  /checkout/endereco   → ['cart-not-empty']
 *  /checkout/frete      → ['enderecoEntregaId']
 *  /checkout/pagamento  → ['opcaoFreteCodigo']
 *  /checkout/revisao    → ['formaPagamentoId']
 *
 * Carrinho vazio em qualquer passo → /carrinho.
 * Pré-requisito faltando → redireciona para o último passo válido.
 */
type StepRequirement = keyof CheckoutState | 'cart-not-empty';

export function checkoutStepGuard(required: StepRequirement[]): CanActivateFn {
  return () => {
    const cart = inject(CartService);
    const state = inject(CheckoutStateService);
    const router = inject(Router);

    if (cart.isEmpty()) {
      router.navigate(['/carrinho']);
      return false;
    }

    const s = state.state();
    for (const req of required) {
      if (req === 'cart-not-empty') continue;
      if (!s[req]) {
        router.navigate([destinoPara(req)]);
        return false;
      }
    }
    return true;
  };
}

function destinoPara(missing: keyof CheckoutState): string {
  switch (missing) {
    case 'enderecoEntregaId':
    case 'enderecoCobrancaId':
      return '/checkout/endereco';
    case 'opcaoFreteCodigo':
    case 'opcaoFreteSnapshot':
      return '/checkout/frete';
    case 'formaPagamentoId':
    case 'parcelas':
      return '/checkout/pagamento';
    default:
      return '/carrinho';
  }
}

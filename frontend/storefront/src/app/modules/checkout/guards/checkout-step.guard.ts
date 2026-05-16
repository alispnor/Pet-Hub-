import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { CartService } from '@modules/cart/services/cart.service';
import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { CheckoutState } from '@modules/checkout/models/checkout';

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

export function checkoutStepGuard(requiredFields: StepRequirement[]): CanActivateFn {
  return () => {
    const cartService = inject(CartService);
    const checkoutState = inject(CheckoutStateService);
    const router = inject(Router);

    if (cartService.isEmpty()) {
      router.navigate(['/carrinho']);
      return false;
    }

    const stateSnapshot = checkoutState.state();
    for (const requirement of requiredFields) {
      if (requirement === 'cart-not-empty') continue;
      if (!stateSnapshot[requirement]) {
        router.navigate([rotaParaPassoFaltante(requirement)]);
        return false;
      }
    }
    return true;
  };
}

function rotaParaPassoFaltante(missingField: keyof CheckoutState): string {
  switch (missingField) {
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

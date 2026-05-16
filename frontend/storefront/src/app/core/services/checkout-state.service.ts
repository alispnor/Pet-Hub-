import { Injectable, signal } from '@angular/core';

import { CheckoutState, EMPTY_CHECKOUT_STATE } from '../models/checkout';

const STORAGE_KEY = 'pethub:checkout:v1';

/**
 * Estado do wizard de checkout. Persiste em sessionStorage (mesma aba),
 * sobrevive a F5, some quando a aba fecha ou após place-order APPROVED.
 *
 * AppSec: dados aqui são apenas IDs (endereço, frete, forma de pagamento) +
 * idempotencyKey gerada com crypto.randomUUID(). NUNCA persistir PAN, CVV
 * ou gatewayToken aqui.
 */
@Injectable({ providedIn: 'root' })
export class CheckoutStateService {
  private readonly _state = signal<CheckoutState>(this.loadFromStorage());

  readonly state = this._state.asReadonly();

  /** Atualiza um subset do state e persiste imediatamente. */
  patch(partial: Partial<CheckoutState>): void {
    const next = { ...this._state(), ...partial };
    this._state.set(next);
    this.persist(next);
  }

  /** Garante que existe uma idempotencyKey; cria com crypto.randomUUID() se faltar. */
  ensureIdempotencyKey(): string {
    const current = this._state().idempotencyKey;
    if (current) return current;
    const key = crypto.randomUUID();
    this.patch({ idempotencyKey: key });
    return key;
  }

  /** Limpa em memória e em sessionStorage. */
  clear(): void {
    this._state.set({ ...EMPTY_CHECKOUT_STATE });
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  }

  private loadFromStorage(): CheckoutState {
    if (typeof sessionStorage === 'undefined') return { ...EMPTY_CHECKOUT_STATE };
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) return { ...EMPTY_CHECKOUT_STATE };
      const parsed = JSON.parse(raw) as Partial<CheckoutState>;
      // Mescla com defaults para tolerar adições de campo no slice 4
      return { ...EMPTY_CHECKOUT_STATE, ...parsed };
    } catch {
      return { ...EMPTY_CHECKOUT_STATE };
    }
  }

  private persist(state: CheckoutState): void {
    if (typeof sessionStorage === 'undefined') return;
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // sessionStorage cheio ou bloqueado — degradação silenciosa, UX volta a "só memória"
    }
  }
}

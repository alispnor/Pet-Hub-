import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '@env/environment';
import {
  AddItemRequest,
  ApplyCouponRequest,
  CartResponse,
  UpdateQtyRequest,
} from '../models/cart';

/**
 * CartService — fonte da verdade do carrinho no client.
 *
 * Estratégia: backend (Redis TTL 30d) é a fonte da verdade. Cada mutação
 * dispara um HTTP e re-seta o signal com a resposta. Sem otimismo local —
 * subtotal/validações vêm prontos da API.
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  private readonly cartSignal = signal<CartResponse | null>(null);

  readonly cart = this.cartSignal.asReadonly();
  readonly totalItens = computed(() => this.cartSignal()?.totalItens ?? 0);
  readonly subtotal = computed(() => this.cartSignal()?.subtotal ?? 0);
  readonly isEmpty = computed(() => (this.cartSignal()?.items.length ?? 0) === 0);

  load(): Observable<CartResponse> {
    return this.http
      .get<CartResponse>(`${this.api}/cart`)
      .pipe(tap((cartResponse) => this.cartSignal.set(cartResponse)));
  }

  add(sku: string, qty: number): Observable<CartResponse> {
    const requestBody: AddItemRequest = { sku, qty };
    return this.http
      .post<CartResponse>(`${this.api}/cart/items`, requestBody)
      .pipe(tap((cartResponse) => this.cartSignal.set(cartResponse)));
  }

  updateQty(sku: string, qty: number): Observable<CartResponse> {
    const requestBody: UpdateQtyRequest = { qty };
    return this.http
      .put<CartResponse>(`${this.api}/cart/items/${encodeURIComponent(sku)}`, requestBody)
      .pipe(tap((cartResponse) => this.cartSignal.set(cartResponse)));
  }

  remove(sku: string): Observable<CartResponse> {
    return this.http
      .delete<CartResponse>(`${this.api}/cart/items/${encodeURIComponent(sku)}`)
      .pipe(tap((cartResponse) => this.cartSignal.set(cartResponse)));
  }

  applyCoupon(codigo: string): Observable<CartResponse> {
    const requestBody: ApplyCouponRequest = { codigo };
    return this.http
      .post<CartResponse>(`${this.api}/cart/coupon`, requestBody)
      .pipe(tap((cartResponse) => this.cartSignal.set(cartResponse)));
  }

  removeCoupon(): Observable<CartResponse> {
    return this.http
      .delete<CartResponse>(`${this.api}/cart/coupon`)
      .pipe(tap((cartResponse) => this.cartSignal.set(cartResponse)));
  }

  /** Limpa local sem chamar backend — usado após place-order APPROVED (o backend já zerou). */
  clearLocal(): void {
    this.cartSignal.set(null);
  }

  /** Limpa local + chama backend. Para "esvaziar carrinho" pelo usuário (não usado no slice 3). */
  clear(): Observable<void> {
    return this.http.delete<void>(`${this.api}/cart`).pipe(tap(() => this.cartSignal.set(null)));
  }
}

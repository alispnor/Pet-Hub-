import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import {
  CreateFormaPagamentoRequest,
  FormaPagamentoResponse,
  TokenizeCardRequest,
  TokenizeCardResponse,
} from '../models/payment-method';

@Injectable({ providedIn: 'root' })
export class PaymentMethodService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(): Observable<FormaPagamentoResponse[]> {
    return this.http.get<FormaPagamentoResponse[]>(`${this.api}/customers/me/payment-methods`);
  }

  create(req: CreateFormaPagamentoRequest): Observable<FormaPagamentoResponse> {
    return this.http.post<FormaPagamentoResponse>(`${this.api}/customers/me/payment-methods`, req);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/customers/me/payment-methods/${id}`);
  }

  setDefault(id: number): Observable<FormaPagamentoResponse> {
    return this.http.post<FormaPagamentoResponse>(`${this.api}/customers/me/payment-methods/${id}/default`, {});
  }

  /**
   * Trafega PAN + CVV. Deve ser a única request que carrega esses campos.
   */
  tokenize(req: TokenizeCardRequest): Observable<TokenizeCardResponse> {
    return this.http.post<TokenizeCardResponse>(
      `${this.api}/customers/payment-methods/tokenize`,
      req,
    );
  }
}

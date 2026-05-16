import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import {
  CheckoutPreviewRequest,
  CheckoutPreviewResponse,
  PlaceOrderRequest,
  PlaceOrderResponse,
} from '../models/checkout';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  preview(req: CheckoutPreviewRequest): Observable<CheckoutPreviewResponse> {
    return this.http.post<CheckoutPreviewResponse>(`${this.api}/checkout/preview`, req);
  }

  placeOrder(req: PlaceOrderRequest): Observable<PlaceOrderResponse> {
    return this.http.post<PlaceOrderResponse>(`${this.api}/checkout/place-order`, req);
  }
}

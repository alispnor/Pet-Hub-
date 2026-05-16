import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import { OpcaoFrete, ShippingCalculateRequest } from '@shared/models/shipping';

@Injectable({ providedIn: 'root' })
export class ShippingService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  /**
   * Aceita CEP com ou sem máscara — o backend exige apenas 8 dígitos.
   * O storefront limpa máscara antes de enviar.
   */
  calculate(req: ShippingCalculateRequest): Observable<OpcaoFrete[]> {
    return this.http.post<OpcaoFrete[]>(`${this.api}/shipping/calculate`, req);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import {
  PageResponse,
  PedidoResponse,
  PedidoResumoResponse,
  StatusPedido,
  Timeline,
} from '../models/order';

export interface ListarPedidosParams {
  page?: number;
  size?: number;
  status?: StatusPedido;
  dataInicio?: string;
  dataFim?: string;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(params: ListarPedidosParams = {}): Observable<PageResponse<PedidoResumoResponse>> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.dataInicio) httpParams = httpParams.set('dataInicio', params.dataInicio);
    if (params.dataFim) httpParams = httpParams.set('dataFim', params.dataFim);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    return this.http.get<PageResponse<PedidoResumoResponse>>(`${this.api}/orders`, { params: httpParams });
  }

  detalhe(numeroPedido: string): Observable<PedidoResponse> {
    return this.http.get<PedidoResponse>(`${this.api}/orders/${numeroPedido}`);
  }

  timeline(numeroPedido: string): Observable<Timeline> {
    return this.http.get<Timeline>(`${this.api}/orders/${numeroPedido}/timeline`);
  }
}

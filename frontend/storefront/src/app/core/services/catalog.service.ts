import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Categoria, ProdutoDetail, ProdutoSummary } from '../models/catalog';
import { PageableResponse } from '../models/page';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  listProducts(opts: { categoria?: string; page?: number; size?: number; sort?: string } = {}):
      Observable<PageableResponse<ProdutoSummary>> {
    let params = new HttpParams();
    if (opts.categoria) params = params.set('categoria', opts.categoria);
    if (opts.page !== undefined) params = params.set('page', opts.page);
    if (opts.size !== undefined) params = params.set('size', opts.size);
    if (opts.sort) params = params.set('sort', opts.sort);
    return this.http.get<PageableResponse<ProdutoSummary>>(`${this.api}/catalog/products`, { params });
  }

  searchProducts(q: string, opts: { page?: number; size?: number } = {}):
      Observable<PageableResponse<ProdutoSummary>> {
    let params = new HttpParams().set('q', q);
    if (opts.page !== undefined) params = params.set('page', opts.page);
    if (opts.size !== undefined) params = params.set('size', opts.size);
    return this.http.get<PageableResponse<ProdutoSummary>>(`${this.api}/catalog/products/search`, { params });
  }

  getProductBySku(sku: string): Observable<ProdutoDetail> {
    return this.http.get<ProdutoDetail>(`${this.api}/catalog/products/${encodeURIComponent(sku)}`);
  }

  listCategories(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.api}/catalog/categories`);
  }
}

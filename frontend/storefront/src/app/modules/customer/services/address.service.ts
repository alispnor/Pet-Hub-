import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import {
  CreateEnderecoRequest,
  EnderecoResponse,
  SetDefaultRequest,
  UpdateEnderecoRequest,
  ViaCepResponse,
} from '../models/address';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(): Observable<EnderecoResponse[]> {
    return this.http.get<EnderecoResponse[]>(`${this.api}/customers/me/addresses`);
  }

  create(req: CreateEnderecoRequest): Observable<EnderecoResponse> {
    return this.http.post<EnderecoResponse>(`${this.api}/customers/me/addresses`, req);
  }

  update(id: number, req: UpdateEnderecoRequest): Observable<EnderecoResponse> {
    return this.http.put<EnderecoResponse>(`${this.api}/customers/me/addresses/${id}`, req);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/customers/me/addresses/${id}`);
  }

  setDefault(id: number, req: SetDefaultRequest): Observable<EnderecoResponse> {
    return this.http.post<EnderecoResponse>(`${this.api}/customers/me/addresses/${id}/default`, req);
  }

  /** CEP com 8 dígitos numéricos, sem máscara. */
  lookupCep(cep: string): Observable<ViaCepResponse> {
    return this.http.get<ViaCepResponse>(`${this.api}/customers/cep/${cep}`);
  }
}

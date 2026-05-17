import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import { PerfilResponse, UpdatePerfilRequest } from '../models/perfil';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  get(): Observable<PerfilResponse> {
    return this.http.get<PerfilResponse>(`${this.api}/customers/me/profile`);
  }

  update(req: UpdatePerfilRequest): Observable<PerfilResponse> {
    return this.http.put<PerfilResponse>(`${this.api}/customers/me/profile`, req);
  }
}

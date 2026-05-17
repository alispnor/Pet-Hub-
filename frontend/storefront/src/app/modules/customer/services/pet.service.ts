import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import { CreatePetRequest, PetResponse, UpdatePetRequest } from '../models/pet';

@Injectable({ providedIn: 'root' })
export class PetService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(): Observable<PetResponse[]> {
    return this.http.get<PetResponse[]>(`${this.api}/customers/me/pets`);
  }

  create(req: CreatePetRequest): Observable<PetResponse> {
    return this.http.post<PetResponse>(`${this.api}/customers/me/pets`, req);
  }

  update(id: number, req: UpdatePetRequest): Observable<PetResponse> {
    return this.http.put<PetResponse>(`${this.api}/customers/me/pets/${id}`, req);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/customers/me/pets/${id}`);
  }

  uploadPhoto(id: number, file: File): Observable<PetResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<PetResponse>(`${this.api}/customers/me/pets/${id}/photo`, formData);
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TacheEntretienRequest, TacheEntretienResponse } from '../models/entretien.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EntretienService {
  private readonly API = `${environment.apiUrl}/entretien`;

  constructor(private http: HttpClient) {}

  create(request: TacheEntretienRequest): Observable<TacheEntretienResponse> {
    return this.http.post<TacheEntretienResponse>(this.API, request);
  }

  findAll(statut?: string): Observable<TacheEntretienResponse[]> {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return this.http.get<TacheEntretienResponse[]>(this.API, { params });
  }

  findEnAttente(): Observable<TacheEntretienResponse[]> {
    return this.http.get<TacheEntretienResponse[]>(`${this.API}/en-attente`);
  }

  updateStatut(id: number, statut: string): Observable<TacheEntretienResponse> {
    return this.http.patch<TacheEntretienResponse>(`${this.API}/${id}/statut`, { statut });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }
}

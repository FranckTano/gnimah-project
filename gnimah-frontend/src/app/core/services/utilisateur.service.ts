import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UtilisateurRequest, UtilisateurResponse } from '../models/utilisateur.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UtilisateurService {
  private readonly API = `${environment.apiUrl}/utilisateurs`;

  constructor(private http: HttpClient) {}

  findAll(): Observable<UtilisateurResponse[]> {
    return this.http.get<UtilisateurResponse[]>(this.API);
  }

  create(request: UtilisateurRequest): Observable<UtilisateurResponse> {
    return this.http.post<UtilisateurResponse>(this.API, request);
  }

  update(id: number, request: UtilisateurRequest): Observable<UtilisateurResponse> {
    return this.http.put<UtilisateurResponse>(`${this.API}/${id}`, request);
  }

  toggleActif(id: number): Observable<UtilisateurResponse> {
    return this.http.patch<UtilisateurResponse>(`${this.API}/${id}/toggle-actif`, {});
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.http.patch<void>(`${this.API}/${id}/password`, { newPassword });
  }
}

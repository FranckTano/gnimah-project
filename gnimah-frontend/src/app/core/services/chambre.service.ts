import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChambreRequest, ChambreResponse } from '../models/chambre.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ChambreService {
  private readonly API = `${environment.apiUrl}/chambres`;

  constructor(private http: HttpClient) {}

  create(request: ChambreRequest): Observable<ChambreResponse> {
    return this.http.post<ChambreResponse>(this.API, request);
  }

  update(id: number, request: ChambreRequest): Observable<ChambreResponse> {
    return this.http.put<ChambreResponse>(`${this.API}/${id}`, request);
  }

  updateEtat(id: number, etat: string): Observable<ChambreResponse> {
    return this.http.patch<ChambreResponse>(`${this.API}/${id}/etat`, { etat });
  }

  toggleActif(id: number): Observable<ChambreResponse> {
    return this.http.patch<ChambreResponse>(`${this.API}/${id}/actif`, {});
  }

  findAll(): Observable<ChambreResponse[]> {
    return this.http.get<ChambreResponse[]>(this.API);
  }

  findAllAdmin(): Observable<ChambreResponse[]> {
    return this.http.get<ChambreResponse[]>(`${this.API}/toutes`);
  }

  findDisponibles(): Observable<ChambreResponse[]> {
    return this.http.get<ChambreResponse[]>(`${this.API}/disponibles`);
  }

  getById(id: number): Observable<ChambreResponse> {
    return this.http.get<ChambreResponse>(`${this.API}/${id}`);
  }
}

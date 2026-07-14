import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EvenementRequest, EvenementResponse } from '../models/evenement.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EvenementService {
  private readonly API = `${environment.apiUrl}/evenements`;

  constructor(private http: HttpClient) {}

  create(request: EvenementRequest): Observable<EvenementResponse> {
    return this.http.post<EvenementResponse>(this.API, request);
  }

  update(id: number, request: EvenementRequest): Observable<EvenementResponse> {
    return this.http.put<EvenementResponse>(`${this.API}/${id}`, request);
  }

  findAll(): Observable<EvenementResponse[]> {
    return this.http.get<EvenementResponse[]>(this.API);
  }

  findByMois(annee: number, mois: number): Observable<EvenementResponse[]> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<EvenementResponse[]>(`${this.API}/calendrier`, { params });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }
}

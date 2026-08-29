import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActionVersementRequest, CompteFinancierRequest, CompteFinancierResponse,
  RecetteAVerserResponse, VersementRequest, VersementResponse
} from '../models/versement.model';
import { PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class VersementService {
  private readonly API = `${environment.apiUrl}/versements`;
  private readonly REF = `${environment.apiUrl}/referentiel`;

  constructor(private http: HttpClient) {}

  findAll(page = 0, size = 20): Observable<PageResponse<VersementResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<VersementResponse>>(this.API, { params });
  }

  getById(id: number): Observable<VersementResponse> {
    return this.http.get<VersementResponse>(`${this.API}/${id}`);
  }

  findPeriode(debut: string, fin: string): Observable<VersementResponse[]> {
    const params = new HttpParams().set('debut', debut).set('fin', fin);
    return this.http.get<VersementResponse[]>(`${this.API}/periode`, { params });
  }

  recetteAVerser(caisseId: number | null, debut: string, fin: string): Observable<RecetteAVerserResponse> {
    let params = new HttpParams().set('debut', debut).set('fin', fin);
    if (caisseId) params = params.set('caisseId', caisseId);
    return this.http.get<RecetteAVerserResponse>(`${this.API}/recette-a-verser`, { params });
  }

  preparer(request: VersementRequest): Observable<VersementResponse> {
    return this.http.post<VersementResponse>(this.API, request);
  }

  deposer(id: number, request?: ActionVersementRequest): Observable<VersementResponse> {
    return this.http.post<VersementResponse>(`${this.API}/${id}/depot`, request ?? {});
  }

  confirmer(id: number, request?: ActionVersementRequest): Observable<VersementResponse> {
    return this.http.post<VersementResponse>(`${this.API}/${id}/confirmation`, request ?? {});
  }

  annuler(id: number, request?: ActionVersementRequest): Observable<VersementResponse> {
    return this.http.post<VersementResponse>(`${this.API}/${id}/annulation`, request ?? {});
  }

  // ---- Comptes de destination ----

  comptes(): Observable<CompteFinancierResponse[]> {
    return this.http.get<CompteFinancierResponse[]>(`${this.REF}/comptes`);
  }

  creerCompte(request: CompteFinancierRequest): Observable<CompteFinancierResponse> {
    return this.http.post<CompteFinancierResponse>(`${this.REF}/comptes`, request);
  }

  modifierCompte(id: number, request: CompteFinancierRequest): Observable<CompteFinancierResponse> {
    return this.http.put<CompteFinancierResponse>(`${this.REF}/comptes/${id}`, request);
  }

  desactiverCompte(id: number): Observable<void> {
    return this.http.delete<void>(`${this.REF}/comptes/${id}`);
  }
}

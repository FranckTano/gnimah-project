import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DepenseRequest, DepenseResponse, FournisseurRequest, FournisseurResponse, SyntheseComptable
} from '../models/comptabilite.model';
import { PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ComptabiliteService {
  private readonly API = `${environment.apiUrl}/comptabilite`;
  private readonly DEPENSES = `${environment.apiUrl}/depenses`;
  private readonly REF = `${environment.apiUrl}/referentiel`;

  constructor(private http: HttpClient) {}

  synthese(debut: string, fin: string): Observable<SyntheseComptable> {
    const params = new HttpParams().set('debut', debut).set('fin', fin);
    return this.http.get<SyntheseComptable>(`${this.API}/synthese`, { params });
  }

  // ---- Dépenses ----

  findDepenses(page = 0, size = 20): Observable<PageResponse<DepenseResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<DepenseResponse>>(this.DEPENSES, { params });
  }

  findDepensesPeriode(debut: string, fin: string): Observable<DepenseResponse[]> {
    const params = new HttpParams().set('debut', debut).set('fin', fin);
    return this.http.get<DepenseResponse[]>(`${this.DEPENSES}/periode`, { params });
  }

  findDepensesEnAttente(): Observable<DepenseResponse[]> {
    return this.http.get<DepenseResponse[]>(`${this.DEPENSES}/en-attente`);
  }

  creerDepense(request: DepenseRequest): Observable<DepenseResponse> {
    return this.http.post<DepenseResponse>(this.DEPENSES, request);
  }

  modifierDepense(id: number, request: DepenseRequest): Observable<DepenseResponse> {
    return this.http.put<DepenseResponse>(`${this.DEPENSES}/${id}`, request);
  }

  validerDepense(id: number): Observable<DepenseResponse> {
    return this.http.post<DepenseResponse>(`${this.DEPENSES}/${id}/validation`, {});
  }

  rejeterDepense(id: number, motif?: string): Observable<DepenseResponse> {
    return this.http.post<DepenseResponse>(`${this.DEPENSES}/${id}/rejet`, { motif });
  }

  reglerDepense(id: number, caisseId?: number | null): Observable<DepenseResponse> {
    return this.http.post<DepenseResponse>(`${this.DEPENSES}/${id}/reglement`, { caisseId });
  }

  supprimerDepense(id: number): Observable<void> {
    return this.http.delete<void>(`${this.DEPENSES}/${id}`);
  }

  // ---- Fournisseurs ----

  fournisseurs(type?: string): Observable<FournisseurResponse[]> {
    let params = new HttpParams();
    if (type) params = params.set('type', type);
    return this.http.get<FournisseurResponse[]>(`${this.REF}/fournisseurs`, { params });
  }

  creerFournisseur(request: FournisseurRequest): Observable<FournisseurResponse> {
    return this.http.post<FournisseurResponse>(`${this.REF}/fournisseurs`, request);
  }

  modifierFournisseur(id: number, request: FournisseurRequest): Observable<FournisseurResponse> {
    return this.http.put<FournisseurResponse>(`${this.REF}/fournisseurs/${id}`, request);
  }

  desactiverFournisseur(id: number): Observable<void> {
    return this.http.delete<void>(`${this.REF}/fournisseurs/${id}`);
  }
}

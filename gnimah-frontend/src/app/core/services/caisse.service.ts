import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CaisseRequest, CaisseResponse, FermetureCaisseRequest, MouvementCaisseRequest,
  MouvementCaisseResponse, OuvertureCaisseRequest, SessionCaisseResponse
} from '../models/caisse.model';
import { PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CaisseService {
  private readonly API = `${environment.apiUrl}/caisse`;

  constructor(private http: HttpClient) {}

  // ---- Caisses ----

  findAll(): Observable<CaisseResponse[]> {
    return this.http.get<CaisseResponse[]>(`${this.API}/caisses`);
  }

  getById(id: number): Observable<CaisseResponse> {
    return this.http.get<CaisseResponse>(`${this.API}/caisses/${id}`);
  }

  create(request: CaisseRequest): Observable<CaisseResponse> {
    return this.http.post<CaisseResponse>(`${this.API}/caisses`, request);
  }

  update(id: number, request: CaisseRequest): Observable<CaisseResponse> {
    return this.http.put<CaisseResponse>(`${this.API}/caisses/${id}`, request);
  }

  // ---- Sessions ----

  ouvrir(request: OuvertureCaisseRequest): Observable<SessionCaisseResponse> {
    return this.http.post<SessionCaisseResponse>(`${this.API}/sessions/ouvrir`, request);
  }

  fermer(sessionId: number, request: FermetureCaisseRequest): Observable<SessionCaisseResponse> {
    return this.http.post<SessionCaisseResponse>(`${this.API}/sessions/${sessionId}/fermer`, request);
  }

  sessionsOuvertes(): Observable<SessionCaisseResponse[]> {
    return this.http.get<SessionCaisseResponse[]>(`${this.API}/sessions/ouvertes`);
  }

  sessionOuverte(caisseId: number): Observable<SessionCaisseResponse | null> {
    return this.http.get<SessionCaisseResponse | null>(`${this.API}/caisses/${caisseId}/session`);
  }

  historiqueSessions(caisseId: number, page = 0, size = 20): Observable<PageResponse<SessionCaisseResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<SessionCaisseResponse>>(`${this.API}/caisses/${caisseId}/sessions`, { params });
  }

  // ---- Mouvements ----

  enregistrerMouvement(request: MouvementCaisseRequest): Observable<MouvementCaisseResponse> {
    return this.http.post<MouvementCaisseResponse>(`${this.API}/mouvements`, request);
  }

  mouvementsSession(sessionId: number): Observable<MouvementCaisseResponse[]> {
    return this.http.get<MouvementCaisseResponse[]>(`${this.API}/sessions/${sessionId}/mouvements`);
  }

  mouvementsCaisse(caisseId: number, page = 0, size = 20): Observable<PageResponse<MouvementCaisseResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<MouvementCaisseResponse>>(`${this.API}/caisses/${caisseId}/mouvements`, { params });
  }

  journal(debut: string, fin: string): Observable<MouvementCaisseResponse[]> {
    const params = new HttpParams().set('debut', debut).set('fin', fin);
    return this.http.get<MouvementCaisseResponse[]>(`${this.API}/journal`, { params });
  }
}

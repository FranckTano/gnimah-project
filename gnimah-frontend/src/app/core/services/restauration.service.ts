import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ArticleRestaurationRequest, ArticleRestaurationResponse, CommandeRequest,
  CommandeResponse, PaiementCommandeRequest
} from '../models/restauration.model';
import { PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RestaurationService {
  private readonly API = `${environment.apiUrl}/restauration`;

  constructor(private http: HttpClient) {}

  // ---- Carte ----

  carte(tout = false): Observable<ArticleRestaurationResponse[]> {
    const params = new HttpParams().set('tout', tout);
    return this.http.get<ArticleRestaurationResponse[]>(`${this.API}/carte`, { params });
  }

  creerArticle(request: ArticleRestaurationRequest): Observable<ArticleRestaurationResponse> {
    return this.http.post<ArticleRestaurationResponse>(`${this.API}/carte`, request);
  }

  modifierArticle(id: number, request: ArticleRestaurationRequest): Observable<ArticleRestaurationResponse> {
    return this.http.put<ArticleRestaurationResponse>(`${this.API}/carte/${id}`, request);
  }

  basculerDisponibilite(id: number): Observable<ArticleRestaurationResponse> {
    return this.http.patch<ArticleRestaurationResponse>(`${this.API}/carte/${id}/disponibilite`, {});
  }

  supprimerArticle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/carte/${id}`);
  }

  // ---- Commandes ----

  commandes(page = 0, size = 20): Observable<PageResponse<CommandeResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<CommandeResponse>>(`${this.API}/commandes`, { params });
  }

  commandesEnCours(): Observable<CommandeResponse[]> {
    return this.http.get<CommandeResponse[]>(`${this.API}/commandes/en-cours`);
  }

  getCommande(id: number): Observable<CommandeResponse> {
    return this.http.get<CommandeResponse>(`${this.API}/commandes/${id}`);
  }

  commandesParSejour(sejourId: number): Observable<CommandeResponse[]> {
    return this.http.get<CommandeResponse[]>(`${this.API}/commandes/sejour/${sejourId}`);
  }

  creerCommande(request: CommandeRequest): Observable<CommandeResponse> {
    return this.http.post<CommandeResponse>(`${this.API}/commandes`, request);
  }

  modifierCommande(id: number, request: CommandeRequest): Observable<CommandeResponse> {
    return this.http.put<CommandeResponse>(`${this.API}/commandes/${id}`, request);
  }

  changerStatut(id: number, statut: string): Observable<CommandeResponse> {
    return this.http.patch<CommandeResponse>(`${this.API}/commandes/${id}/statut`, { statut });
  }

  encaisser(id: number, request: PaiementCommandeRequest): Observable<CommandeResponse> {
    return this.http.post<CommandeResponse>(`${this.API}/commandes/${id}/encaissement`, request);
  }

  annulerCommande(id: number, motif?: string): Observable<CommandeResponse> {
    return this.http.post<CommandeResponse>(`${this.API}/commandes/${id}/annulation`, { motif });
  }
}

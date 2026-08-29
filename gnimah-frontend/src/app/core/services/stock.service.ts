import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ArticleStockRequest, ArticleStockResponse, MouvementStockRequest, MouvementStockResponse
} from '../models/stock.model';
import { PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly API = `${environment.apiUrl}/stock`;

  constructor(private http: HttpClient) {}

  articles(categorie?: string): Observable<ArticleStockResponse[]> {
    let params = new HttpParams();
    if (categorie) params = params.set('categorie', categorie);
    return this.http.get<ArticleStockResponse[]>(`${this.API}/articles`, { params });
  }

  alertes(): Observable<ArticleStockResponse[]> {
    return this.http.get<ArticleStockResponse[]>(`${this.API}/alertes`);
  }

  valeur(): Observable<{ valeur: number }> {
    return this.http.get<{ valeur: number }>(`${this.API}/valeur`);
  }

  creer(request: ArticleStockRequest): Observable<ArticleStockResponse> {
    return this.http.post<ArticleStockResponse>(`${this.API}/articles`, request);
  }

  modifier(id: number, request: ArticleStockRequest): Observable<ArticleStockResponse> {
    return this.http.put<ArticleStockResponse>(`${this.API}/articles/${id}`, request);
  }

  desactiver(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/articles/${id}`);
  }

  enregistrerMouvement(request: MouvementStockRequest): Observable<MouvementStockResponse> {
    return this.http.post<MouvementStockResponse>(`${this.API}/mouvements`, request);
  }

  historiqueArticle(id: number): Observable<MouvementStockResponse[]> {
    return this.http.get<MouvementStockResponse[]>(`${this.API}/articles/${id}/mouvements`);
  }

  journal(page = 0, size = 20): Observable<PageResponse<MouvementStockResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<MouvementStockResponse>>(`${this.API}/mouvements`, { params });
  }
}

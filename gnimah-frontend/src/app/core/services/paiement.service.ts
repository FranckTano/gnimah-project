import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaiementRequest, PaiementResponse } from '../models/paiement.model';
import { PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PaiementService {
  private readonly API = `${environment.apiUrl}/paiements`;

  constructor(private http: HttpClient) {}

  enregistrer(request: PaiementRequest): Observable<PaiementResponse> {
    return this.http.post<PaiementResponse>(this.API, request);
  }

  findAll(page = 0, size = 20): Observable<PageResponse<PaiementResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<PaiementResponse>>(this.API, { params });
  }

  findBySejour(sejourId: number): Observable<PaiementResponse[]> {
    return this.http.get<PaiementResponse[]>(`${this.API}/sejour/${sejourId}`);
  }

  telechargerRecu(id: number): Observable<Blob> {
    return this.http.get(`${this.API}/${id}/recu`, { responseType: 'blob' });
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AcompteRequest, AnnulationRequest, PolitiqueAnnulation,
  ReservationRequest, ReservationResponse
} from '../models/reservation.model';
import { PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly API = `${environment.apiUrl}/reservations`;

  constructor(private http: HttpClient) {}

  create(request: ReservationRequest): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(this.API, request);
  }

  updateStatut(id: number, statut: string): Observable<ReservationResponse> {
    return this.http.patch<ReservationResponse>(`${this.API}/${id}/statut`, { statut });
  }

  getById(id: number): Observable<ReservationResponse> {
    return this.http.get<ReservationResponse>(`${this.API}/${id}`);
  }

  findAll(page = 0, size = 20): Observable<PageResponse<ReservationResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<ReservationResponse>>(this.API, { params });
  }

  findArriveesDuJour(): Observable<ReservationResponse[]> {
    return this.http.get<ReservationResponse[]>(`${this.API}/arrivees-jour`);
  }

  /**
   * Encaisse l'acompte. C'est ce geste qui confirme la réservation : il n'y a
   * plus de confirmation « sèche » sans argent reçu.
   */
  enregistrerAcompte(id: number, request: AcompteRequest): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${this.API}/${id}/acompte`, request);
  }

  /** Simule l'annulation (pénalité, remboursement) sans rien modifier. */
  politiqueAnnulation(id: number): Observable<PolitiqueAnnulation> {
    return this.http.get<PolitiqueAnnulation>(`${this.API}/${id}/politique-annulation`);
  }

  annuler(id: number, request?: AnnulationRequest): Observable<ReservationResponse> {
    return this.http.post<ReservationResponse>(`${this.API}/${id}/annulation`, request ?? {});
  }
}

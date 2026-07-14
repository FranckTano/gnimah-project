import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SejourRequest, SejourResponse } from '../models/sejour.model';
import { PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SejourService {
  private readonly API = `${environment.apiUrl}/sejours`;

  constructor(private http: HttpClient) {}

  checkIn(request: SejourRequest): Observable<SejourResponse> {
    return this.http.post<SejourResponse>(`${this.API}/check-in`, request);
  }

  checkOut(id: number): Observable<SejourResponse> {
    return this.http.post<SejourResponse>(`${this.API}/${id}/check-out`, {});
  }

  getById(id: number): Observable<SejourResponse> {
    return this.http.get<SejourResponse>(`${this.API}/${id}`);
  }

  findAll(page = 0, size = 20): Observable<PageResponse<SejourResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<SejourResponse>>(this.API, { params });
  }

  findEnCours(): Observable<SejourResponse[]> {
    return this.http.get<SejourResponse[]>(`${this.API}/en-cours`);
  }

  findDepartsJour(): Observable<SejourResponse[]> {
    return this.http.get<SejourResponse[]>(`${this.API}/departs-jour`);
  }

  findByClient(clientId: number, page = 0, size = 10): Observable<PageResponse<SejourResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<SejourResponse>>(`${this.API}/client/${clientId}`, { params });
  }
}

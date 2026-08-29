import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IncidentRequest, IncidentResponse, ResolutionIncidentRequest } from '../models/incident.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private readonly API = `${environment.apiUrl}/incidents`;

  constructor(private http: HttpClient) {}

  findAll(): Observable<IncidentResponse[]> {
    return this.http.get<IncidentResponse[]>(this.API);
  }

  findOuverts(): Observable<IncidentResponse[]> {
    return this.http.get<IncidentResponse[]>(`${this.API}/ouverts`);
  }

  getById(id: number): Observable<IncidentResponse> {
    return this.http.get<IncidentResponse>(`${this.API}/${id}`);
  }

  findByChambre(chambreId: number): Observable<IncidentResponse[]> {
    return this.http.get<IncidentResponse[]>(`${this.API}/chambre/${chambreId}`);
  }

  declarer(request: IncidentRequest): Observable<IncidentResponse> {
    return this.http.post<IncidentResponse>(this.API, request);
  }

  prendreEnCharge(id: number): Observable<IncidentResponse> {
    return this.http.post<IncidentResponse>(`${this.API}/${id}/prise-en-charge`, {});
  }

  resoudre(id: number, request: ResolutionIncidentRequest): Observable<IncidentResponse> {
    return this.http.post<IncidentResponse>(`${this.API}/${id}/resolution`, request);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }
}

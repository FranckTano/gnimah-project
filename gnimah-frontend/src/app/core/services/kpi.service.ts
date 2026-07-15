import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KpiResponse } from '../models/kpi.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KpiService {
  private readonly API = `${environment.apiUrl}/kpi`;

  constructor(private http: HttpClient) {}

  /** debut/fin are plain "YYYY-MM-DD" dates; the backend expects full LocalDateTime strings. */
  getDashboard(debut?: string, fin?: string): Observable<KpiResponse> {
    let params = new HttpParams();
    if (debut) params = params.set('debut', `${debut}T00:00:00`);
    if (fin) params = params.set('fin', `${fin}T23:59:59`);
    return this.http.get<KpiResponse>(`${this.API}/dashboard`, { params });
  }

  getDashboardToday(): Observable<KpiResponse> {
    return this.http.get<KpiResponse>(`${this.API}/dashboard/today`);
  }
}

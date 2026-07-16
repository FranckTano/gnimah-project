import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificationResponse } from '../models/notification.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly API = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  findRecentes(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(this.API);
  }

  countNonLues(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.API}/non-lues/count`);
  }

  marquerLue(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API}/${id}/lu`, {});
  }

  marquerToutesLues(): Observable<void> {
    return this.http.patch<void>(`${this.API}/lu-toutes`, {});
  }
}

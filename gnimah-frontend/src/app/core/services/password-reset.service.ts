import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PasswordResetRequestResponse, ResoudreReinitialisationResponse } from '../models/password-reset.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PasswordResetService {
  private readonly API = `${environment.apiUrl}/password-reset-requests`;

  constructor(private http: HttpClient) {}

  findEnAttente(): Observable<PasswordResetRequestResponse[]> {
    return this.http.get<PasswordResetRequestResponse[]>(this.API);
  }

  resoudre(id: number): Observable<ResoudreReinitialisationResponse> {
    return this.http.post<ResoudreReinitialisationResponse>(`${this.API}/${id}/resoudre`, {});
  }
}

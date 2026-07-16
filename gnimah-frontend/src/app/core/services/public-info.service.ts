import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicHotelInfo } from '../models/public-info.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PublicInfoService {
  private readonly API = `${environment.apiUrl}/public`;

  constructor(private http: HttpClient) {}

  getHotelInfo(): Observable<PublicHotelInfo> {
    return this.http.get<PublicHotelInfo>(`${this.API}/hotel-info`);
  }
}

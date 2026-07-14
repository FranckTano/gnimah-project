import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClientRequest, ClientResponse, PageResponse } from '../models/client.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly API = `${environment.apiUrl}/clients`;

  constructor(private http: HttpClient) {}

  create(request: ClientRequest): Observable<ClientResponse> {
    return this.http.post<ClientResponse>(this.API, request);
  }

  update(id: number, request: ClientRequest): Observable<ClientResponse> {
    return this.http.put<ClientResponse>(`${this.API}/${id}`, request);
  }

  getById(id: number): Observable<ClientResponse> {
    return this.http.get<ClientResponse>(`${this.API}/${id}`);
  }

  search(search?: string, page = 0, size = 20): Observable<PageResponse<ClientResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    return this.http.get<PageResponse<ClientResponse>>(this.API, { params });
  }

  findAll(page = 0, size = 20): Observable<PageResponse<ClientResponse>> {
    return this.search(undefined, page, size);
  }

  findByTelephone(telephone: string): Observable<ClientResponse> {
    return this.http.get<ClientResponse>(`${this.API}/by-telephone/${telephone}`);
  }

  findByPiece(numeroPiece: string): Observable<ClientResponse> {
    return this.http.get<ClientResponse>(`${this.API}/by-piece/${numeroPiece}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }
}

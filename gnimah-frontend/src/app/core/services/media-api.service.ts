import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MediaResidenceRequest, MediaResidenceResponse } from '../models/media.model';

/** Accès REST à la galerie de la résidence, administrable depuis l'application. */
@Injectable({ providedIn: 'root' })
export class MediaApiService {
  private readonly API = `${environment.apiUrl}/medias`;

  constructor(private http: HttpClient) {}

  /** Visuels actifs, dans l'ordre d'affichage. */
  findVisibles(): Observable<MediaResidenceResponse[]> {
    return this.http.get<MediaResidenceResponse[]>(this.API);
  }

  /** Vue d'administration : inclut les visuels masqués. */
  findAll(): Observable<MediaResidenceResponse[]> {
    return this.http.get<MediaResidenceResponse[]>(`${this.API}/admin`);
  }

  /**
   * Indique si le serveur sait héberger un fichier. Sans identifiants Cloudinary
   * côté serveur, seule la saisie d'une adresse externe reste possible : le
   * formulaire masque alors l'envoi de fichier au lieu de laisser l'utilisateur
   * buter sur une erreur.
   */
  capacites(): Observable<{ hebergementDisponible: boolean }> {
    return this.http.get<{ hebergementDisponible: boolean }>(`${this.API}/capacites`);
  }

  televerser(fichier: File, titre: string, categorie: string): Observable<MediaResidenceResponse> {
    const form = new FormData();
    form.append('fichier', fichier);
    form.append('titre', titre);
    form.append('categorie', categorie);
    return this.http.post<MediaResidenceResponse>(`${this.API}/upload`, form);
  }

  creerDepuisUrl(request: MediaResidenceRequest): Observable<MediaResidenceResponse> {
    return this.http.post<MediaResidenceResponse>(this.API, request);
  }

  update(id: number, request: MediaResidenceRequest): Observable<MediaResidenceResponse> {
    return this.http.put<MediaResidenceResponse>(`${this.API}/${id}`, request);
  }

  /** `sens` négatif remonte le visuel, positif le descend. */
  deplacer(id: number, sens: -1 | 1): Observable<MediaResidenceResponse[]> {
    return this.http.patch<MediaResidenceResponse[]>(
      `${this.API}/${id}/deplacer`, null, { params: new HttpParams().set('sens', sens) });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }
}

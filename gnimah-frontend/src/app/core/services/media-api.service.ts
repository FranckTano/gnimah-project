import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CapacitesMedia, MediaResidenceRequest, MediaResidenceResponse } from '../models/media.model';

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

  /** Par quelle voie téléverser : signée par le serveur, ou directe depuis le navigateur. */
  capacites(): Observable<CapacitesMedia> {
    return this.http.get<CapacitesMedia>(`${this.API}/capacites`);
  }

  /**
   * Envoie une photo choisie dans la galerie ou l'explorateur de fichiers.
   *
   * Deux voies, transparentes pour l'utilisateur :
   *  — si le serveur détient les clés Cloudinary, le fichier transite par lui,
   *    qui signe l'envoi. Le secret ne quitte jamais le serveur ;
   *  — sinon le navigateur dépose le fichier directement sur Cloudinary avec le
   *    préréglage non signé, puis fait enregistrer l'adresse obtenue. Cela évite
   *    d'exiger une configuration serveur pour une action aussi courante.
   *
   * Dans les deux cas le résultat est le même : le fichier est sur Cloudinary et
   * son lien est en base.
   */
  televerser(
    fichier: File, titre: string, categorie: string, capacites: CapacitesMedia
  ): Observable<MediaResidenceResponse> {
    if (capacites.hebergementDisponible) {
      const form = new FormData();
      form.append('fichier', fichier);
      form.append('titre', titre);
      form.append('categorie', categorie);
      return this.http.post<MediaResidenceResponse>(`${this.API}/upload`, form);
    }

    if (!capacites.cloudName || !capacites.uploadPreset) {
      return throwError(() => ({
        error: {
          message: "L'envoi de photos n'est pas configuré. Renseignez les identifiants "
            + 'Cloudinary sur le serveur, ou utilisez « Depuis une adresse web ».'
        }
      }));
    }

    return from(this.deposerSurCloudinary(fichier, capacites)).pipe(
      switchMap(depot => this.http.post<MediaResidenceResponse>(`${this.API}/televersement`, {
        titre, categorie, url: depot.url, urlVignette: depot.urlVignette, publicId: depot.publicId
      } as MediaResidenceRequest))
    );
  }

  /**
   * Dépôt direct navigateur → Cloudinary.
   *
   * L'identifiant est préfixé du dossier de la galerie : le serveur refuse tout
   * enregistrement hors de ce dossier, ce qui empêche de lui faire adopter — puis
   * supprimer — un objet quelconque du compte.
   */
  private async deposerSurCloudinary(
    fichier: File, cap: CapacitesMedia
  ): Promise<{ url: string; urlVignette: string; publicId: string }> {
    const base = (fichier.name.replace(/\.[^.]+$/, '') || 'photo')
      .normalize('NFD').replace(/[̀-ͯ]/g, '')
      .toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')
      .slice(0, 60) || 'photo';
    const publicId = `${cap.dossier}/${Date.now()}-${base}`;

    const form = new FormData();
    form.append('file', fichier);
    form.append('upload_preset', cap.uploadPreset);
    form.append('public_id', publicId);

    const rep = await fetch(`https://api.cloudinary.com/v1_1/${cap.cloudName}/image/upload`, {
      method: 'POST', body: form
    });
    const json = await rep.json().catch(() => ({}));
    if (!rep.ok) {
      throw { error: { message: json?.error?.message || "L'envoi de l'image a échoué." } };
    }

    const idDistant: string = json.public_id;
    // `f_auto,q_auto` laisse Cloudinary livrer du WebP/AVIF selon le navigateur.
    const livraison = (t: string) =>
      `https://res.cloudinary.com/${cap.cloudName}/image/upload/${t}/${idDistant}`;
    return {
      url: livraison('f_auto,q_auto'),
      urlVignette: livraison('f_auto,q_auto,w_620,c_limit'),
      publicId: idDistant
    };
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

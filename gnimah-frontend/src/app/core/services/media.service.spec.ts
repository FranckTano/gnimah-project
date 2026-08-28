import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MediaService } from './media.service';
import { MediaResidenceResponse } from '../models/media.model';
import { environment } from '../../../environments/environment';

/**
 * Le service construit ses URL une seule fois, à la construction : chaque cas
 * réinitialise donc l'injecteur après avoir posé la configuration voulue.
 */
function service(): { media: MediaService; http: HttpTestingController } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
  return { media: TestBed.inject(MediaService), http: TestBed.inject(HttpTestingController) };
}

function visuel(p: Partial<MediaResidenceResponse>): MediaResidenceResponse {
  return {
    id: 1, titre: 'Photo', categorie: 'Chambres',
    url: 'https://cdn.test/a.jpg', urlVignette: 'https://cdn.test/a-sm.jpg',
    publicId: 'gnimah/a', position: 0, actif: true, misEnAvant: false,
    heberge: true, createdAt: null, ...p
  };
}

describe('MediaService', () => {
  const original = { ...environment.media };

  afterEach(() => {
    environment.media.photos = original.photos;
    environment.media.video = original.video;
  });

  describe('résolution des URL du catalogue intégré', () => {
    it('sert les fichiers embarqués quand aucun CDN n\'est configuré', () => {
      environment.media.photos = '';
      environment.media.video = '';
      const { media } = service();

      expect(media.heros.src).toBe('/media/photos/facade-patio.jpg');
      expect(media.heros.thumb).toBe('/media/photos/facade-patio-sm.jpg');
      expect(media.video.src).toBe('/media/video/gnimah-presentation.mp4');
    });

    it('bascule sur le CDN dès que les racines sont renseignées', () => {
      environment.media.photos = 'https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/gnimah';
      environment.media.video = 'https://res.cloudinary.com/demo/video/upload/q_auto/gnimah';
      const { media } = service();

      expect(media.heros.src).toContain('/gnimah/photos/facade-patio.jpg');
      expect(media.video.src).toContain('/gnimah/video/gnimah-presentation.mp4');
    });

    it('range l\'affiche vidéo avec la vidéo, mais la sert comme une image', () => {
      environment.media.photos = 'https://cdn.test/img';
      environment.media.video = 'https://cdn.test/vid';
      const { media } = service();
      // Cloudinary sépare /image/upload/ et /video/upload/ : une affiche JPEG
      // passe par la racine « images » tout en restant dans le dossier « video ».
      expect(media.video.poster).toBe('https://cdn.test/img/video/gnimah-presentation-poster.jpg');
    });

    it('tolère une barre oblique finale dans la configuration', () => {
      environment.media.photos = 'https://cdn.test/img/';
      environment.media.video = 'https://cdn.test/vid/';
      const { media } = service();
      expect(media.heros.src).toBe('https://cdn.test/img/photos/facade-patio.jpg');
    });
  });

  describe('galerie administrée', () => {
    it('démarre sur le catalogue intégré, avant toute réponse de l\'API', () => {
      const { media } = service();
      expect(media.photos.length).toBe(14);
      expect(media.heros.id).toBe('facade-patio');
    });

    it('remplace le catalogue par la galerie renvoyée', () => {
      const { media } = service();
      media.appliquer([
        visuel({ id: 7, titre: 'Terrasse', categorie: 'Extérieur' }),
        visuel({ id: 8, titre: 'Piscine', categorie: 'Extérieur', url: 'https://cdn.test/b.jpg' })
      ]);
      expect(media.photos.length).toBe(2);
      expect(media.photos[0].titre).toBe('Terrasse');
    });

    it('écarte les visuels masqués', () => {
      const { media } = service();
      media.appliquer([
        visuel({ id: 1, titre: 'Visible' }),
        visuel({ id: 2, titre: 'Masquée', actif: false })
      ]);
      expect(media.photos.map(p => p.titre)).toEqual(['Visible']);
    });

    it('retient le visuel mis en avant comme héros', () => {
      const { media } = service();
      media.appliquer([
        visuel({ id: 1, titre: 'Premier' }),
        visuel({ id: 2, titre: 'Vedette', misEnAvant: true })
      ]);
      expect(media.heros.titre).toBe('Vedette');
    });

    it('retombe sur le premier visuel si aucun n\'est mis en avant', () => {
      const { media } = service();
      media.appliquer([visuel({ id: 1, titre: 'Premier' }), visuel({ id: 2, titre: 'Second' })]);
      expect(media.heros.titre).toBe('Premier');
    });

    it('revient au catalogue intégré si la galerie est vidée', () => {
      const { media } = service();
      media.appliquer([visuel({ id: 1 })]);
      expect(media.photos.length).toBe(1);
      // Une galerie vidée par mégarde ne doit pas dépouiller toute l'application.
      media.appliquer([]);
      expect(media.photos.length).toBe(14);
      expect(media.heros.id).toBe('facade-patio');
    });

    it('publie chaque mise à jour aux abonnés', () => {
      const { media } = service();
      const recues: number[] = [];
      media.photos$.subscribe(p => recues.push(p.length));
      media.appliquer([visuel({ id: 1 }), visuel({ id: 2 })]);
      expect(recues).toEqual([14, 2]);
    });

    it('garde le catalogue intégré si l\'API échoue', () => {
      const { media, http } = service();
      media.charger();
      http.expectOne(r => r.url.endsWith('/medias')).flush('boom', { status: 500, statusText: 'Erreur' });
      expect(media.photos.length).toBe(14);
    });

    it('sert toujours le catalogue intégré au diaporama de connexion', () => {
      const { media } = service();
      // L'écran de connexion n'est pas authentifié : il ne peut pas lire l'API.
      media.appliquer([visuel({ id: 1, titre: 'Ajoutée après coup' })]);
      expect(media.diaporamaConnexion.length).toBe(5);
      expect(media.diaporamaConnexion[0].id).toBe('facade-patio');
    });
  });

  describe('visuel par défaut d\'une chambre', () => {
    it('donne toujours la même photo à une chambre donnée', () => {
      const { media } = service();
      expect(media.photoChambre('STANDARD', '102').id).toBe(media.photoChambre('STANDARD', '102').id);
    });

    it('puise dans la catégorie « Chambres » quand elle existe', () => {
      const { media } = service();
      media.appliquer([
        visuel({ id: 1, titre: 'Façade', categorie: 'Extérieur' }),
        visuel({ id: 2, titre: 'Chambre A', categorie: 'Chambres' }),
        visuel({ id: 3, titre: 'Chambre B', categorie: 'Chambres' })
      ]);
      for (const num of ['101', '102', '203', '304']) {
        expect(media.photoChambre('STANDARD', num).categorie).toBe('Chambres');
      }
    });

    it('retombe sur toute la galerie si aucune photo n\'est classée « Chambres »', () => {
      const { media } = service();
      media.appliquer([visuel({ id: 1, titre: 'Façade', categorie: 'Extérieur' })]);
      expect(media.photoChambre('SUITE', '203').titre).toBe('Façade');
    });

    it('intègre les visuels ajoutés par l\'administrateur à la rotation', () => {
      const { media } = service();
      const galerie = Array.from({ length: 6 }, (_, i) =>
        visuel({ id: i + 1, titre: `Chambre ${i}`, categorie: 'Chambres' }));
      media.appliquer(galerie);
      const tires = new Set(
        ['101', '102', '103', '104', '105', '106', '201', '202']
          .map(n => media.photoChambre('STANDARD', n).titre));
      // La graine doit répartir, pas retomber toujours sur le même visuel.
      expect(tires.size).toBeGreaterThan(1);
    });
  });
});

import { TestBed } from '@angular/core/testing';
import { MediaService } from './media.service';
import { environment } from '../../../environments/environment';

/**
 * Le service construit ses URL une seule fois, à la construction : chaque cas
 * réinitialise donc l'injecteur après avoir posé la configuration voulue.
 */
function service(): MediaService {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({});
  return TestBed.inject(MediaService);
}

describe('MediaService — résolution des URL', () => {
  const original = { ...environment.media };

  afterEach(() => {
    environment.media.photos = original.photos;
    environment.media.video = original.video;
  });

  it('sert les fichiers embarqués quand aucun CDN n\'est configuré', () => {
    environment.media.photos = '';
    environment.media.video = '';
    const m = service();

    expect(m.heros.src).toBe('/media/photos/facade-patio.jpg');
    expect(m.heros.thumb).toBe('/media/photos/facade-patio-sm.jpg');
    expect(m.video.src).toBe('/media/video/gnimah-presentation.mp4');
    expect(m.video.poster).toBe('/media/video/gnimah-presentation-poster.jpg');
  });

  it('bascule sur le CDN dès que les racines sont renseignées', () => {
    environment.media.photos = 'https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/gnimah';
    environment.media.video = 'https://res.cloudinary.com/demo/video/upload/q_auto/gnimah';
    const m = service();

    expect(m.heros.src).toBe('https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/gnimah/photos/facade-patio.jpg');
    expect(m.video.src).toBe('https://res.cloudinary.com/demo/video/upload/q_auto/gnimah/video/gnimah-presentation.mp4');
  });

  it('range l\'affiche vidéo avec la vidéo, mais la sert comme une image', () => {
    environment.media.photos = 'https://cdn.test/img';
    environment.media.video = 'https://cdn.test/vid';
    const m = service();

    // Cloudinary sépare /image/upload/ et /video/upload/ : une affiche JPEG doit
    // passer par la racine « images » tout en restant dans le dossier « video ».
    expect(m.video.poster).toBe('https://cdn.test/img/video/gnimah-presentation-poster.jpg');
  });

  it('tolère une barre oblique finale dans la configuration', () => {
    environment.media.photos = 'https://cdn.test/img/';
    environment.media.video = 'https://cdn.test/vid/';
    const m = service();

    expect(m.heros.src).toBe('https://cdn.test/img/photos/facade-patio.jpg');
    expect(m.video.src).toBe('https://cdn.test/vid/video/gnimah-presentation.mp4');
  });

  it('donne toujours la même photo à une chambre donnée', () => {
    environment.media.photos = '';
    environment.media.video = '';
    const m = service();

    expect(m.photoChambre('STANDARD', '102').id).toBe(m.photoChambre('STANDARD', '102').id);
    expect(m.photoChambre('SUITE', '203').categorie).toBe('Chambres');
  });
});

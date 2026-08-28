export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',

  /**
   * Racines des médias (photos et vidéo de la résidence).
   *
   * Laissées vides, l'application sert les fichiers de `public/media/` qu'elle
   * embarque : tout fonctionne hors ligne, sans compte ni réseau. Renseignées,
   * elles pointent vers le CDN et les fichiers cessent de peser sur le déploiement.
   *
   * Cloudinary sépare les images des vidéos dans ses URL (`/image/upload/` et
   * `/video/upload/`) : il faut donc deux racines, et non une seule.
   *
   *   photos : https://res.cloudinary.com/<cloud>/image/upload/f_auto,q_auto/gnimah
   *   video  : https://res.cloudinary.com/<cloud>/video/upload/q_auto/gnimah
   *
   * `f_auto,q_auto` laisse Cloudinary livrer du WebP ou de l'AVIF selon le
   * navigateur : les photos arrivent alors plus légères qu'en JPEG.
   *
   * En développement, on garde volontairement le local — inutile de consommer
   * du quota CDN à chaque rechargement.
   */
  media: {
    photos: '',
    video: ''
  }
};

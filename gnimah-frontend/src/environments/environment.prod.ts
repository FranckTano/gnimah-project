export const environment = {
  production: true,
  apiUrl: '/api',

  /**
   * Racines des médias servies en production.
   *
   * Renseignez-les avec les valeurs imprimées par `npm run media:upload` pour
   * que photos et vidéo soient livrées par le CDN plutôt que par le serveur de
   * l'application. Vides, l'app retombe sur les fichiers de `public/media/` :
   * elle reste donc fonctionnelle même si le CDN est injoignable.
   *
   *   photos : https://res.cloudinary.com/<cloud>/image/upload/f_auto,q_auto/gnimah
   *   video  : https://res.cloudinary.com/<cloud>/video/upload/q_auto/gnimah
   */
  media: {
    photos: 'https://res.cloudinary.com/dehzeeo6p/image/upload/f_auto,q_auto/gnimah',
    video: 'https://res.cloudinary.com/dehzeeo6p/video/upload/q_auto/gnimah'
  }
};

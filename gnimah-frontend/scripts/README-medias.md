# Médias de la résidence — héberger sur Cloudinary

Les photos et la vidéo de présentation vivent dans `public/media/`
(2 Mo de photos + 7,2 Mo de vidéo). L'application peut les servir elle-même,
ou les faire livrer par un CDN pour alléger le déploiement.

## Comment l'application choisit

`src/environments/environment*.ts` porte deux racines :

```ts
media: {
  photos: '',   // vide → fichiers locaux de public/media/
  video:  ''
}
```

- **Vides** → l'app sert `public/media/`. Fonctionne hors ligne, sans compte.
- **Renseignées** → l'app construit toutes ses URL sur le CDN.

Le repli est volontaire : si le CDN tombe ou si son quota est épuisé, il suffit
de revider les deux champs pour que l'application redevienne complète.

> Cloudinary sépare les images des vidéos dans ses URL (`/image/upload/` et
> `/video/upload/`), d'où **deux** racines et non une seule. L'affiche de la
> vidéo est un JPEG : elle passe par la racine `photos` tout en restant rangée
> dans le dossier `video`.

## Envoyer les fichiers

Deux modes. Le premier évite de faire circuler le secret d'API.

### Mode non signé — recommandé

Aucun secret ne quitte le compte Cloudinary.

1. Console Cloudinary → **Settings → Upload → Upload presets → Add upload preset**.
2. Réglages à poser :
   - **Signing Mode** : `Unsigned`
   - **Unique filename** : `Off` ← indispensable
   - **Folder** : laisser vide
   - Nom du préréglage : par exemple `gnimah_media`
3. Depuis `gnimah-frontend/` :

   ```bash
   CLOUDINARY_CLOUD_NAME=xxx CLOUDINARY_UPLOAD_PRESET=gnimah_media npm run media:upload
   ```

   En PowerShell :

   ```powershell
   $env:CLOUDINARY_CLOUD_NAME="xxx"; $env:CLOUDINARY_UPLOAD_PRESET="gnimah_media"
   npm run media:upload
   ```

> **Pourquoi « Unique filename » doit être désactivé** : laissé actif, Cloudinary
> accole un suffixe aléatoire à chaque nom (`facade-patio-a7f3c1.jpg`). Les
> fichiers seraient bien en ligne, mais l'application, qui construit ses URL par
> convention, ne les trouverait pas. Le script détecte ce cas et s'arrête avec le
> message correspondant plutôt que de laisser passer des pages sans visuel.

### Mode signé

Permet de relancer l'envoi sans créer de doublons (Cloudinary interdit
l'écrasement en mode non signé).

1. **Settings → API Keys**, relevez `API Key` et `API Secret`.
2. Lancez :

   ```bash
   CLOUDINARY_CLOUD_NAME=xxx CLOUDINARY_API_KEY=xxx CLOUDINARY_API_SECRET=xxx \
     npm run media:upload
   ```

Le secret donne un **accès complet au compte**. Ne le committez jamais, et
faites-le tourner (console → API Keys → Rotate) s'il a transité quelque part.

### Ensuite

Le script imprime les deux racines à coller dans
`src/environments/environment.prod.ts`. Les identifiants ne sont jamais écrits
sur disque : le script les lit dans l'environnement du processus.

## Bon à savoir

- En **mode signé**, le script est idempotent (`public_id` stable + `overwrite`) :
  le relancer met à jour les fichiers. En **mode non signé**, Cloudinary refuse
  l'écrasement — pour renvoyer un fichier, supprimez-le d'abord depuis la console.
- **Aucune variable d'environnement n'est nécessaire sur Render ou Vercel.** Les
  URL des médias sont publiques et figées dans le build ; les clés Cloudinary ne
  servent qu'à l'envoi, une opération ponctuelle depuis un poste de développement.
- La racine `photos` recommandée contient `f_auto,q_auto` : Cloudinary livre
  alors du WebP ou de l'AVIF selon le navigateur, donc **plus léger que les
  JPEG actuels** sans rien changer au code.
- Pour ajouter une photo : déposez-la dans `public/media/photos/`, déclarez-la
  dans `src/app/core/services/media.service.ts`, relancez `npm run media:upload`.

## Un autre hébergeur ?

Rien dans le code n'est propre à Cloudinary. Téléversez `public/media/` tel quel
chez n'importe quel hébergeur statique et pointez les deux racines vers le même
dossier :

```ts
media: {
  photos: 'https://mon-cdn.example.com/gnimah',
  video:  'https://mon-cdn.example.com/gnimah'
}
```

L'arborescence attendue est `<racine>/photos/…` et `<racine>/video/…`.

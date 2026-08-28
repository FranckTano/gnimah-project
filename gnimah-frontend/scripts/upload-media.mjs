#!/usr/bin/env node
/**
 * Téléverse les médias de `public/media/` vers Cloudinary, puis imprime les deux
 * racines à coller dans `src/environments/environment.prod.ts`.
 *
 * Utilisation :
 *   CLOUDINARY_CLOUD_NAME=xxx CLOUDINARY_API_KEY=xxx CLOUDINARY_API_SECRET=xxx \
 *     npm run media:upload
 *
 * Les trois valeurs se lisent sur le tableau de bord Cloudinary (Settings →
 * API Keys). Elles ne sont jamais écrites sur disque : le script les prend dans
 * l'environnement et les oublie en sortant.
 *
 * Le script est idempotent : `overwrite: true` avec un `public_id` stable, donc
 * le relancer met à jour les fichiers au lieu d'en créer des doublons.
 */

import { createHash } from 'node:crypto';
import { readdir, readFile } from 'node:fs/promises';
import { basename, extname, join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const RACINE = join(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'media');
const DOSSIER_CLOUD = 'gnimah';

const CLOUD = process.env.CLOUDINARY_CLOUD_NAME;
const CLE = process.env.CLOUDINARY_API_KEY;
const SECRET = process.env.CLOUDINARY_API_SECRET;
const PRESET = process.env.CLOUDINARY_UPLOAD_PRESET;

/**
 * Deux modes, au choix :
 *
 *  — « signé » : clé + secret d'API. Permet l'écrasement, donc des relances
 *    idempotentes. Le secret donne un accès complet au compte : à réserver à un
 *    poste de confiance, et à faire tourner s'il a circulé.
 *
 *  — « non signé » : un simple nom de préréglage, créé dans la console
 *    (Settings → Upload → Upload presets → Signing Mode: Unsigned). Aucun secret
 *    ne quitte le compte. Cloudinary interdit en revanche l'écrasement en mode
 *    non signé : une relance créera des doublons plutôt que de mettre à jour.
 */
const MODE_SIGNE = Boolean(CLE && SECRET);

if (!CLOUD || (!MODE_SIGNE && !PRESET)) {
  console.error(`
  Configuration Cloudinary manquante.

  Mode signé (relançable, écrase les fichiers existants) :
    CLOUDINARY_CLOUD_NAME=... CLOUDINARY_API_KEY=... CLOUDINARY_API_SECRET=... \\
      npm run media:upload

  Mode non signé (aucun secret à transmettre) — créez d'abord un préréglage
  « Unsigned » dans Settings → Upload → Upload presets, puis :
    CLOUDINARY_CLOUD_NAME=... CLOUDINARY_UPLOAD_PRESET=... \\
      npm run media:upload
`);
  process.exit(1);
}

/** Cloudinary signe en SHA-1 les paramètres triés, secret concaténé à la fin. */
function signer(params) {
  const aSigner = Object.keys(params)
    .sort()
    .map((k) => `${k}=${params[k]}`)
    .join('&');
  return createHash('sha1').update(aSigner + SECRET).digest('hex');
}

async function televerser(chemin, typeRessource, dossier) {
  const nom = basename(chemin, extname(chemin));
  const publicId = `${DOSSIER_CLOUD}/${dossier}/${nom}`;

  const form = new FormData();
  form.append('file', new Blob([await readFile(chemin)]));
  form.append('public_id', publicId);

  if (MODE_SIGNE) {
    const horodatage = Math.floor(Date.now() / 1000);
    form.append('api_key', CLE);
    form.append('timestamp', String(horodatage));
    form.append('overwrite', 'true');
    form.append('signature', signer({ overwrite: 'true', public_id: publicId, timestamp: horodatage }));
  } else {
    // `overwrite` est refusé sur un envoi non signé : on ne le transmet pas.
    form.append('upload_preset', PRESET);
  }

  const rep = await fetch(
    `https://api.cloudinary.com/v1_1/${CLOUD}/${typeRessource}/upload`,
    { method: 'POST', body: form }
  );
  const json = await rep.json();
  if (!rep.ok) {
    throw new Error(`${basename(chemin)} — ${json?.error?.message || rep.statusText}`);
  }

  // L'application construit ses URL par convention : le `public_id` renvoyé doit
  // être exactement celui demandé. Un préréglage laissé sur « Unique filename »
  // y accole un suffixe aléatoire — les images seraient bien en ligne, mais
  // introuvables. Mieux vaut échouer ici que livrer des pages sans visuel.
  if (json.public_id !== publicId) {
    throw new Error(
      `${basename(chemin)} — Cloudinary a renommé le fichier en « ${json.public_id} » ` +
      `au lieu de « ${publicId} ». Dans le préréglage « ${PRESET} », désactivez ` +
      `« Unique filename » (et laissez « Folder » vide), puis relancez.`
    );
  }
  return json;
}

async function fichiersDe(sousDossier, extensions) {
  const dossier = join(RACINE, sousDossier);
  const noms = await readdir(dossier);
  return noms
    .filter((n) => extensions.includes(extname(n).toLowerCase()))
    .map((n) => join(dossier, n));
}

const photos = await fichiersDe('photos', ['.jpg', '.jpeg', '.png', '.webp']);
const affiches = await fichiersDe('video', ['.jpg', '.jpeg', '.png']);
const videos = await fichiersDe('video', ['.mp4', '.webm', '.mov']);

console.log(`\n  ${photos.length} photo(s), ${affiches.length} affiche(s), ${videos.length} vidéo(s)`
  + ` à envoyer vers « ${CLOUD} » — mode ${MODE_SIGNE ? 'signé' : 'non signé'}.\n`);

let envoyes = 0;
let octets = 0;
const echecs = [];

for (const [liste, type, dossier] of [
  [photos, 'image', 'photos'],
  [affiches, 'image', 'video'],
  [videos, 'video', 'video']
]) {
  for (const chemin of liste) {
    try {
      const r = await televerser(chemin, type, dossier);
      envoyes++;
      octets += r.bytes || 0;
      console.log(`  ✓ ${basename(chemin)}  →  ${r.secure_url}`);
    } catch (e) {
      echecs.push(e.message);
      console.error(`  ✗ ${e.message}`);
    }
  }
}

console.log(`\n  ${envoyes} fichier(s) envoyé(s), ${(octets / 1024 / 1024).toFixed(2)} Mo.`);

if (echecs.length) {
  console.error(`\n  ${echecs.length} échec(s) — rien n'a été modifié dans la configuration.\n`);
  process.exit(1);
}

console.log(`
  Collez ces deux racines dans src/environments/environment.prod.ts :

  media: {
    photos: 'https://res.cloudinary.com/${CLOUD}/image/upload/f_auto,q_auto/${DOSSIER_CLOUD}',
    video:  'https://res.cloudinary.com/${CLOUD}/video/upload/q_auto/${DOSSIER_CLOUD}'
  }

  « f_auto,q_auto » laisse Cloudinary livrer du WebP/AVIF selon le navigateur :
  les photos arriveront plus légères qu'en JPEG.

  Les fichiers de public/media/ restent en place comme secours : si ces deux
  champs sont vides, l'application les sert elle-même.
`);

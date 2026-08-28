#!/usr/bin/env node
/**
 * Génère une police d'icônes réduite aux seules icônes réellement employées.
 *
 * Pourquoi : les icônes venaient d'un CDN externe (jsdelivr). Quand ce CDN est
 * lent ou bloqué — ce qui arrive selon les réseaux —, TOUTE l'interface perd ses
 * icônes : les boutons deviennent des ronds vides. La police est donc désormais
 * servie par l'application elle-même.
 *
 * Mais la police complète pèse 820 Ko pour ~5900 glyphes, alors que le projet en
 * utilise moins d'une centaine. Ce script parcourt les sources, relève les
 * classes `ti-*` employées, et produit une police et une feuille de style qui ne
 * contiennent qu'elles.
 *
 * Lancé automatiquement avant chaque build (`prebuild` dans package.json) :
 * ajouter une icône dans un template suffit, la recompilation la prend en compte.
 */

import { readFile, writeFile, readdir, mkdir } from 'node:fs/promises';
import { join, dirname, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import subsetFont from 'subset-font';

const RACINE = join(dirname(fileURLToPath(import.meta.url)), '..');
const SOURCE = join(RACINE, 'src');
const PAQUET = join(RACINE, 'node_modules', '@tabler', 'icons-webfont', 'dist');
const SORTIE = join(RACINE, 'src', 'assets', 'icons');

/** Parcourt récursivement les sources et renvoie leur contenu concaténé. */
async function lireSources(dossier) {
  let texte = '';
  for (const entree of await readdir(dossier, { withFileTypes: true })) {
    const chemin = join(dossier, entree.name);
    if (entree.isDirectory()) {
      texte += await lireSources(chemin);
    } else if (['.html', '.ts', '.scss'].includes(extname(entree.name))) {
      texte += await readFile(chemin, 'utf8');
    }
  }
  return texte;
}

const sources = await lireSources(SOURCE);

// `ti-loader-2`, `ti-chevron-left`… On exclut `ti-` seul et la classe de base.
const utilisees = new Set(
  [...sources.matchAll(/\bti-([a-z0-9]+(?:-[a-z0-9]+)*)\b/g)].map(m => m[1])
);
// Ces deux-là servent d'animation/état, pas d'icône.
utilisees.delete('spin');

// Le CSS du paquet associe chaque nom à son point de code : c'est la source de
// vérité, on ne devine jamais un code.
const cssComplet = await readFile(join(PAQUET, 'tabler-icons.min.css'), 'utf8');
const codes = new Map();
for (const m of cssComplet.matchAll(/\.ti-([a-z0-9-]+):before\{content:"\\([0-9a-f]+)"\}/g)) {
  codes.set(m[1], m[2]);
}

const retenues = [];
const introuvables = [];
for (const nom of [...utilisees].sort()) {
  if (codes.has(nom)) retenues.push(nom);
  else introuvables.push(nom);
}

if (!retenues.length) {
  console.error("  Aucune icône reconnue — le sous-ensemble serait vide. Abandon.");
  process.exit(1);
}

// Les glyphes à conserver, désignés par leurs caractères.
const glyphes = retenues.map(n => String.fromCodePoint(parseInt(codes.get(n), 16))).join('');

const policeComplete = await readFile(join(PAQUET, 'fonts', 'tabler-icons.woff2'));
const reduite = await subsetFont(policeComplete, glyphes, { targetFormat: 'woff2' });

await mkdir(SORTIE, { recursive: true });
await writeFile(join(SORTIE, 'gnimah-icons.woff2'), reduite);

const css = `/* Police d'icônes réduite — générée par scripts/build-icons.mjs, ne pas éditer.
 * ${retenues.length} icônes retenues sur ${codes.size} disponibles.
 * Servie par l'application : aucune dépendance à un CDN externe, dont
 * l'indisponibilité ferait disparaître toutes les icônes de l'interface. */
@font-face {
  font-family: "tabler-icons";
  font-style: normal;
  font-weight: 400;
  font-display: block;
  src: url("./gnimah-icons.woff2") format("woff2");
}
.ti {
  font-family: "tabler-icons" !important;
  font-style: normal;
  font-weight: normal !important;
  font-variant: normal;
  text-transform: none;
  line-height: 1;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  display: inline-block;
}
${retenues.map(n => `.ti-${n}:before { content: "\\${codes.get(n)}"; }`).join('\n')}
`;
await writeFile(join(SORTIE, 'gnimah-icons.css'), css, 'utf8');

const ko = (n) => `${(n / 1024).toFixed(1)} Ko`;
console.log(`  ${retenues.length} icônes retenues sur ${codes.size} disponibles.`);
console.log(`  Police : ${ko(policeComplete.length)} → ${ko(reduite.length)}`);
console.log(`  CSS    : ${ko(cssComplet.length)} → ${ko(Buffer.byteLength(css))}`);
if (introuvables.length) {
  // Un nom inconnu vient d'une faute de frappe ou d'une icône renommée en amont :
  // sans ce signalement, elle disparaîtrait silencieusement de l'interface.
  console.warn(`\n  ⚠ ${introuvables.length} classe(s) ti-* introuvable(s) dans Tabler :`);
  introuvables.forEach(n => console.warn(`     ti-${n}`));
}

# Vendeur.ci — Design System

> La vitrine des vendeurs ivoiriens.
> Direction visuelle : **« Confiance nette »** — bleu confiance saturé, blanc franc, coins très arrondis, accents sémantiques vifs.

---

## 1. Contexte produit

**Vendeur.ci** est une application **mobile** de mise en relation entre vendeurs et acheteurs en Côte d'Ivoire. Les vendeurs publient leurs produits (photos + vidéos) ; les acheteurs les parcourent librement et contactent les vendeurs **via WhatsApp ou appel direct**. Pas de commande ni de paiement in-app en V1 — la plateforme est une **vitrine structurée et permanente** pour des vendeurs jusque-là dispersés sur WhatsApp.

**Modèle économique** : acheteurs 100% gratuits ; vendeurs en abonnement mensuel (Gratuit / Essentiel 2 000 FCFA / Pro 5 000 FCFA) ; promotions payantes (boost produit, vendeur vedette, bannières).

### Acteurs
| Acteur | Compte | Parcours |
|---|---|---|
| Acheteur visiteur | Non | Navigation libre |
| Acheteur connecté | Oui (OTP) | Favoris synchronisés |
| Vendeur | Oui (OTP obligatoire) | Boutique + publication |
| Admin | Oui | Back-office web (hors périmètre de ce système) |

### Surfaces couvertes par ce design system
- **App mobile** — flow Acheteur (Accueil, Catalogue, Fiche produit, Boutique, Favoris, Profil) + flow Vendeur (Dashboard, Mes produits, Ajout produit, Stats, Plans, Promotions, Profil) + flow Auth (numéro, OTP, onboarding boutique).

### Sources fournies
- **Brief produit complet** « Vendeur.ci — Résumé Design Complet » (collé dans le chat) : architecture de l'app, 17 écrans détaillés, composants UI récurrents, interactions clés, plans tarifaires. C'est la **source de vérité fonctionnelle**.
- Aucun codebase ni fichier Figma n'a été fourni — l'identité visuelle est **créée** par nous (direction « Confiance nette », validée par l'utilisateur parmi 3 pistes : voir `exploration/Directions.html`).
- Palette de départ suggérée dans le brief (bleu `#1B73E8`) : conservée dans l'esprit, légèrement ajustée pour la saturation/contraste (`#1B6FE8`).

---

## 2. Fondamentaux de contenu (copywriting)

La langue de l'app est le **français de Côte d'Ivoire**, clair et direct, jamais corporate.

- **Tutoiement vs vouvoiement** : on **vouvoie** l'utilisateur (« Entrez votre numéro », « Connectez-vous pour sauvegarder vos favoris »). Ton respectueux mais chaleureux.
- **Voix** : orientée action et bénéfice. Verbes à l'impératif sur les boutons : « Recevoir le code », « Contacter sur WhatsApp », « Publier le produit », « Choisir ce plan ».
- **Casse** : phrases en **casse de phrase** (première lettre majuscule, le reste minuscule). Pas de MAJUSCULES criées, sauf micro-eyebrows (« PROMO DE LA SEMAINE ») avec interlettrage élargi. Le nom de marque s'écrit **vendeur.ci** en minuscules dans le logo, **Vendeur.ci** en début de phrase dans le texte courant.
- **Prix** : toujours « **35 000 FCFA** » — espace fine comme séparateur de milliers, devise « FCFA » en suffixe légèrement plus petit/discret. « Prix sur demande » si non renseigné, badge « Négociable » à part.
- **Numéros** : indicatif **+225** fixe, suivi de 10 chiffres groupés (`+225 07 XX XX XX XX`).
- **Messages d'état** : courts et humains. Erreur OTP : « Code invalide, réessayez. » / « Code expiré, demandez un nouveau code. » État vide favoris : « Vous n'avez pas encore de favoris » + CTA « Découvrir des produits ».
- **Message WhatsApp pré-rempli** (cœur du produit) : « Bonjour ! Je suis intéressé(e) par votre produit *[titre]* vu sur Vendeur.ci. Est-il toujours disponible ? » — chaleureux, inclusif (parenthèse e), mentionne la plateforme.
- **Emoji** : usage **très parcimonieux**. Un 👋 dans le « Bonjour, [Prénom] 👋 » du dashboard est toléré (chaleur). Ailleurs, on s'appuie sur les **icônes Tabler**, pas sur les emoji. *(À confirmer : l'utilisateur peut décider de tout retirer.)*
- **Vibe générale** : « ton marché ivoirien moderne » — accessible, fiable, jamais intimidant. On parle de « boutique », « vendeur vedette », « vitrine », pas de jargon e-commerce occidental.

---

## 3. Fondations visuelles

### Couleur
- **Primaire** : bleu confiance `#1B6FE8` (`--color-primary`). Sert aux CTA principaux, prix, liens, états actifs, en-têtes vendeur.
- **Stratégie d'accent** : le bleu est roi. Les accents sémantiques sont **vifs mais réservés à leur rôle** — vert WhatsApp `#25D366` (uniquement le canal WhatsApp), ambre `#F59E0B` (notes/étoiles + badge vedette), rouge `#EF4444` (favori actif + erreurs), vert succès, orange (plan expiré), violet (badge Pro). On ne décore jamais avec ces couleurs hors de leur sens.
- **Fonds** : fond d'app gris très clair `#F4F6FA` (`--surface-page`) ; surfaces (cartes, feuilles) en **blanc franc** `#FFFFFF`. Contraste net surface/fond = lisibilité « punchy ».
- **Pastilles catégories** : monochromes **bleu clair** (`--color-primary-soft`) pour ne pas distraire du produit — c'est ce qui distingue la direction A des autres.
- **Dégradés** : un seul, de marque — `linear-gradient(150deg, #1B6FE8, #2F86FF)` — sur les en-têtes vendeur et bannières promo. Jamais de dégradés violacés/arc-en-ciel.

### Typographie
- **Sora** partout (titres et corps). Géométrique, moderne, rassurante. Titres en **800 (extrabold)** avec interlettrage serré (`-0.02em`) ; corps en 400/500 ; libellés et prix en 700/800.
- Échelle mobile : display 30px, h1 24px, h2 21px, h3 17px, body 15px, small 13.5px, badges 11–12px. **Jamais sous 11px.**
- JetBrains Mono réservé aux rares affichages de code/identifiants techniques.

### Espacement & layout
- Base **4px**. Gouttière d'écran **16px**, espace inter-sections **18px**.
- Densité **aérée** : les sections respirent, beaucoup de blanc, une seule action dominante par écran.
- Layout mobile fixe : status bar → app bar → zone scrollable → bottom nav (toujours visible, 70px). FAB « + » flottant pour le vendeur, ancré bas-droite.

### Formes & rayons (très arrondis)
- Cartes **22px**, boutons/inputs **16px**, feuilles/panneaux **26px**, en-têtes arrondis **32px**, pastilles/badges **100px** (pill), avatars ronds.
- Vignettes produit dans les listes : **14px**. Icônes-boutons : **12–13px**.

### Ombres & élévation
- Système d'ombres **douces et bleutées**, jamais dures.
  - `--shadow-sm` : cartes posées (très subtil).
  - `--shadow-md` : éléments survolés / FAB.
  - `--shadow-lg` : bottom-sheet & modals.
  - `--shadow-nav` : ombre vers le haut sous la bottom nav.
  - `--shadow-primary` / `--shadow-whatsapp` : halo coloré sous les CTA pleins (bleu / vert).
- Pas de bordure sur les cartes par défaut (direction A) — l'élévation se fait à l'ombre. Bordures fines `--border-soft` uniquement pour séparateurs/inputs.

### Animation
- Sorties douces (`--ease-out`, cubic-bezier(0.22,1,0.36,1)), durées 120–320ms.
- Un **petit rebond** (`--ease-spring`) réservé aux moments de plaisir : ajout aux favoris (le cœur « pop »), apparition du FAB, succès d'onboarding.
- Fades pour les transitions d'écran et l'apparition des skeletons. Pas d'animations décoratives en boucle.

### États interactifs
- **Hover** (desktop/preview) : assombrir légèrement (primaire → `--primary-hover` plus clair sur fonds clairs, ou overlay sombre 6%).
- **Press (tap)** : léger **scale 0.97** + assombrissement (`--primary-press`). Le tap doit se sentir.
- **Focus** : anneau `--ring` (halo bleu doux 3px). Toujours visible au clavier.
- **Désactivé** : opacité réduite + couleur `--text-muted`, pas d'ombre.
- **Favori** : cœur vide `--text-disabled` → plein `--color-favorite` (rouge) avec rebond.

### Transparence & flou
- Flou (`backdrop-filter`) réservé : boutons flottants sur photo (fiche produit), badge vidéo sur média, overlay de modal. Sinon surfaces **opaques** pour la lisibilité au soleil (contexte mobile extérieur).

### Imagerie
- Photos produits réelles, **cadrées carré/4:3**, coins arrondis hérités de la carte.
- En l'absence d'image : placeholders dégradés de marque (`.vc-ph-1..6`, voir `tokens/base.css`) avec une icône Tabler fantôme centrée — jamais de zone grise vide.
- Couverture boutique : pleine largeur, photo de profil ronde en surimpression.

---

## 4. Iconographie

- **Système** : **Tabler Icons** (webfont), chargé depuis CDN. Le brief référence explicitement les classes `ti-*` (`ti-home`, `ti-heart`, `ti-layout-grid`, `ti-chart-bar`, `ti-plus`…), c'est donc le set **canonique** — pas une substitution.
- **Chargement** :
  ```html
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@tabler/icons-webfont@3.31.0/dist/tabler-icons.min.css">
  ```
  puis `<i class="ti ti-heart"></i>`. Variantes pleines en `-filled` (`ti-heart-filled`, `ti-star-filled`, `ti-rosette-discount-check-filled`).
- **Style** : trait **outline 2px**, coins arrondis — cohérent avec le ton doux/rassurant. Les icônes « état » se remplissent (favori actif, étoile, badge vérifié).
- **Tailles** : 20–24px en barre/nav, 17–21px dans les boutons, 26–28px pour le FAB.
- **Icônes-clés** : navigation acheteur `ti-home-2 / ti-layout-grid / ti-heart / ti-user` ; navigation vendeur `ti-layout-dashboard / ti-box / ti-chart-bar / ti-user` + FAB `ti-plus` ; contact `ti-brand-whatsapp / ti-phone` ; vedette `ti-rosette-discount-check-filled` ; note `ti-star-filled` ; vidéo `ti-player-play-filled`.
- **Emoji** : non utilisés comme icônes (sauf le 👋 d'accueil, à confirmer). **Unicode déco** : non. Tout passe par Tabler.
- **Logo** : voir `assets/logo.svg` (wordmark + mark storefront), `assets/logo-mark.svg` (mark seul), `assets/logo-white.svg` (sur fond bleu). Le mark est une devanture de boutique stylisée, écho au mot « vendeur ».

---

## 5. Index du système

### Racine
- `styles.css` — point d'entrée global (que des `@import`). **Les consommateurs ne lient que ce fichier.**
- `tokens/` — `colors.css`, `typography.css`, `spacing.css`, `effects.css` (ombres, anneaux, motion, keyframes), `fonts.css`, `base.css`.
- `assets/` — `logo.svg`, `logo-mark.svg`, `logo-white.svg`.
- `exploration/Directions.html` — les 3 pistes visuelles explorées (A retenue).
- `SKILL.md` — mode d'emploi pour réutiliser ce système (compatible Agent Skills).

### Composants (`components/`)
- `core/` — **Button** (incl. variantes WhatsApp / Appel), **IconButton**, **Badge**, **Avatar**, **Chip** (pastille catégorie), **StarRating** (note + saisie d'avis), **Input** (champ arrondi, préfixe +225 / suffixe FCFA).
- `commerce/` — **ProductCard** (carte grille 2 colonnes), **SellerCard** (carte vendeur du carrousel), **StatCard** (tuile statistique du dashboard).
- `navigation/` — **BottomNav** (variantes acheteur / vendeur + FAB), **SearchBar**.

Les pièces de coque non exportées (StatusBar, AppBar, SectionHead, **Toast**, **BottomSheet**, **EmptyState**) vivent dans `ui_kits/mobile-app/ui.jsx` (`window.VKUI`) — propres au kit, pas au système global.

Chaque dossier de composants expose ses exports sous le namespace global (voir `check_design_system`) et porte une carte `@dsCard` de démonstration (`*.card.html`).

### UI kit (`ui_kits/mobile-app/`)
- Recreation interactive de l'app mobile Vendeur.ci, cliquable de bout en bout (mock) : Accueil, Catalogue, Fiche produit, Auth (numéro + OTP), Onboarding boutique, Dashboard vendeur, Mes produits, Ajout produit. Compose les composants du système (`index.html` + `data.js` + `ui.jsx` + `screens-buyer.jsx` + `screens-seller.jsx` + `app.jsx`).

### Specimens (`guidelines/`)
- Cartes de fondation affichées dans l'onglet Design System : **Colors** (bleu, neutres, sémantiques), **Type** (famille, échelle, prix), **Spacing** (échelle 4px, rayons, élévation), **Brand** (logo, placeholders, iconographie).

---

## 6. À faire / questions ouvertes
- **Police** : Sora est servie via Google Fonts. Pour un build 100% hors-ligne, fournir les `.woff2` et basculer `tokens/fonts.css` en `@font-face` locaux.
- **Logo** : logotype créé par nos soins (la marque n'en avait pas). À remplacer si un logo officiel existe.
- **Emoji** : un seul (👋) subsiste, gardé sur demande de l'utilisateur — retirable en un point (dashboard).

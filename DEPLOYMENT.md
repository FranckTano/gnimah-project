# Guide de déploiement — GNIMAH

Backend sur **Render**, frontend sur **Vercel**, base de données sur **Supabase**. Suis les étapes dans cet ordre (la base avant le backend, le backend avant le frontend) — chaque étape a besoin d'informations produites par la précédente.

Avant de commencer : commit et push tout ce qui est en cours (`git status` doit être propre), le déploiement se fait à partir du dépôt GitHub `FranckTano/gnimah-project`.

---

## 1. Supabase — la base de données

1. Sur [supabase.com](https://supabase.com), crée un nouveau projet (région proche de tes utilisateurs, ex. Europe si tes clients sont en Côte d'Ivoire/Europe — Supabase n'a pas de région Afrique, choisis la plus proche en latence).
2. Note le **mot de passe de la base** que tu choisis à la création (tu ne pourras plus le revoir en clair ensuite).
3. Une fois le projet créé : **Project Settings → Database → Connection string**. Prends la variante **"Session pooler"** (port `5432`, pas la "Transaction pooler" sur port `6543`) — c'est la plus compatible avec Hibernate/Spring qui utilise des requêtes préparées ; la "Transaction pooler" peut casser certains comportements JPA.
4. Tu obtiens quelque chose comme :
   ```
   postgresql://postgres.xxxxxxxxxxxx:[TON-MOT-DE-PASSE]@aws-0-eu-west-1.pooler.supabase.com:5432/postgres
   ```
   Transforme-la en URL JDBC pour le backend :
   ```
   jdbc:postgresql://aws-0-eu-west-1.pooler.supabase.com:5432/postgres
   ```
   (garde `postgres.xxxxxxxxxxxx` comme **username**, et ton mot de passe à part — ce sont les 3 valeurs dont Render aura besoin à l'étape suivante.)
5. Tu n'as **rien d'autre à faire manuellement** sur le schéma : au premier démarrage, le backend applique automatiquement les migrations Flyway (tables, colonnes, 12 chambres, 2 caisses, galerie) puis crée les comptes `admin`, `directeur`, `responsable`, `agent` via `DataInitializer`. Les **données de démonstration** (faux clients, séjours, paiements) ne sont plus créées : elles ne le sont que si la variable `DEMO_DATA_ENABLED=true` est posée — à ne jamais faire en production.

---

## 2. Render — le backend

### 2.1 Créer le service

**Option A — Blueprint (recommandé)** : un fichier `render.yaml` est déjà prêt à la racine du dépôt.
1. Dashboard Render → **New → Blueprint** → connecte le dépôt GitHub → Render détecte `render.yaml` et propose de créer le service `gnimah-backend` (Docker, à partir de `gnimah-backend/Dockerfile`).
2. Render générera automatiquement un `JWT_SECRET` aléatoire et robuste (ne réutilise jamais celui de `docker-compose.yml`, qui est un secret de dev connu de quiconque lit le dépôt).

**Option B — manuel** : New → Web Service → connecte le dépôt → Runtime **Docker** → Root Directory `gnimah-backend` → Dockerfile Path `gnimah-backend/Dockerfile`.

### 2.2 Variables d'environnement

Que tu aies utilisé le Blueprint ou pas, va dans **Environment** et vérifie/complète :

| Variable | Valeur |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host-pooler-supabase>:5432/postgres` |
| `SPRING_DATASOURCE_USERNAME` | `postgres.xxxxxxxxxxxx` (fourni par Supabase) |
| `SPRING_DATASOURCE_PASSWORD` | le mot de passe choisi à la création du projet Supabase |
| `JWT_SECRET` | généré par le Blueprint, sinon génère-en un toi-même (32+ caractères aléatoires) |
| `CORS_ALLOWED_ORIGINS` | l'URL Vercel une fois connue (étape 3) — laisse `http://localhost:4200` pour l'instant, tu la mettras à jour après |
| `DEMO_DATA_ENABLED` | *facultatif* — `false` par défaut. Ne jamais mettre `true` en production (faux séjours et faux paiements) |
| `LOG_LEVEL_APP` | *facultatif* — `INFO` par défaut ; `DEBUG` le temps d'un diagnostic |
| `KEEPALIVE_TARGET_URL` | *facultatif* — `https://<ton-service>.onrender.com/api/actuator/health` : le serveur se « ping » lui-même toutes les 13 min pour ne pas s'endormir |

Render fournit déjà `PORT` automatiquement — le backend l'utilise (`server.port: ${PORT:8080}` dans `application.yml`), rien à faire.

#### Cloudinary (facultatif) — envoi de photos depuis l'application

Ces trois variables activent l'envoi de fichier dans **Administration → Photos de
la résidence**. Sans elles, l'application fonctionne normalement : la page reste
utilisable pour référencer une image déjà en ligne, et l'écran l'explique.

| Variable | Où la trouver |
|---|---|
| `CLOUDINARY_CLOUD_NAME` | console Cloudinary → **Settings → API Keys** → *Cloud name* |
| `CLOUDINARY_API_KEY` | même page → *API Key* |
| `CLOUDINARY_API_SECRET` | même page → *API Secret* (bouton « révéler ») |

**Marche à suivre**

1. Render → ton service `gnimah-backend` → onglet **Environment**.
2. **Add Environment Variable**, une par une, puis **Save Changes**.
3. Render redéploie tout seul (~2 min).
4. Vérifie : connecte-toi en directeur, va sur **Administration → Photos de la
   résidence**. Le bandeau orange « L'envoi de fichier n'est pas disponible »
   doit avoir disparu et la zone de dépôt apparaître à la place.

**Pourquoi ces clés vivent sur le serveur et pas dans le frontend**

L'envoi transite par le backend, qui signe la requête Cloudinary. L'autre approche
— téléverser depuis le navigateur avec un préréglage « non signé » — obligerait à
publier le nom du préréglage dans le bundle : n'importe quel visiteur pourrait
alors déposer des fichiers sur le compte. Ici le secret ne quitte jamais Render,
et seul un directeur authentifié peut déclencher un envoi.

⚠️ Le secret donne un **accès complet** au compte Cloudinary. Ne le committe
jamais, et fais-le tourner (console → API Keys → *Rotate*) s'il a circulé.

> Les **adresses publiques** des photos, elles, n'ont rien de secret : elles sont
> figées dans le build du frontend (`environment.prod.ts`) et apparaissent dans le
> HTML livré au navigateur. Aucune variable Cloudinary n'est à poser sur Vercel.

### 2.3 Déployer et vérifier

1. Lance le déploiement. Le premier build prend quelques minutes (Gradle + build de l'image Docker).
2. Une fois "Live", teste `https://<ton-service>.onrender.com/api/actuator/health` → doit répondre `{"status":"UP"}`.
3. Regarde les logs de démarrage : tu dois voir les 7 migrations Flyway appliquées, puis `Utilisateurs déjà initialisés` ou la création des comptes de démo.
4. Note l'URL Render finale (`https://gnimah-backend-xxxx.onrender.com`) — tu en as besoin à l'étape 3.

⚠️ **Plan gratuit Render** : le service s'endort après 15 min d'inactivité et met ~30-50s à se réveiller au premier appel suivant. Normal, pas un bug — prévisible pour une petite résidence hôtelière, mais informe tes utilisateurs si le premier chargement de la journée semble lent.

---

## 3. Vercel — le frontend

1. Sur [vercel.com](https://vercel.com) : **Add New → Project** → importe le dépôt GitHub.
2. Root Directory : `gnimah-frontend`.
3. Framework Preset : laisse "Other" (Angular n'a pas de preset dédié pour le nouveau builder) — le fichier `vercel.json` déjà présent dans `gnimah-frontend/` définit `buildCommand` (`npm run build`) et `outputDirectory` (`dist/gnimah-frontend/browser`), Vercel les reprendra automatiquement.
4. **Avant le premier déploiement**, ouvre `gnimah-frontend/vercel.json` et remplace le placeholder par ta vraie URL Render obtenue à l'étape 2.3 :
   ```json
   { "source": "/api/(.*)", "destination": "https://gnimah-backend-xxxx.onrender.com/api/$1" }
   ```
   Commit et push ce changement.
5. Déploie. Vercel te donne une URL du type `https://gnimah-project.vercel.app`.

### Pourquoi ce `vercel.json` plutôt qu'une URL d'API en dur dans le code

`gnimah-frontend/src/environments/environment.prod.ts` pointe vers `/api` (chemin relatif). Le `rewrites` de `vercel.json` fait proxy à la volée : le navigateur appelle `https://gnimah-project.vercel.app/api/...` (même origine que la page), et Vercel relaie côté serveur vers Render. Résultat : aucun problème de CORS côté navigateur (tout semble same-origin), et si l'URL Render change un jour, un seul fichier à modifier.

### 3.1 Mettre à jour CORS côté Render

Retourne dans les variables d'environnement Render (étape 2.2) et mets à jour :
```
CORS_ALLOWED_ORIGINS=https://gnimah-project.vercel.app
```
(plusieurs origines séparées par des virgules si tu as aussi un domaine personnalisé). Redéploie le backend pour que ça prenne effet. Cette valeur reste utile en défense en profondeur même si le proxy Vercel évite la plupart des appels cross-origin directs (ex. si quelqu'un teste l'API Swagger directement depuis son navigateur).

---

## 4. Vérification post-déploiement

Checklist à dérouler une fois les deux services live :

- [ ] `https://<backend>.onrender.com/api/actuator/health` répond `UP`
- [ ] `https://<backend>.onrender.com/api/public/hotel-info` renvoie la `version` attendue (2.1.0) et une `dateBuild` récente — preuve que Render sert le dernier code ; la même version s'affiche sous le formulaire de connexion
- [ ] `https://<frontend>.vercel.app/login` affiche le vrai nombre de chambres (pas de valeur figée) — confirme que le proxy `/api` fonctionne
- [ ] Connexion avec `admin` / `Admin@2026` (ou tes propres identifiants si tu as changé les comptes de démo) fonctionne et redirige vers le tableau de bord
- [ ] Un rechargement de page sur une route profonde (ex. `/entretien`) ne donne pas un 404 Vercel (vérifie le `rewrites` catch-all vers `index.html`)
- [ ] Le bouton "Nouvelle tâche" apparaît pour `responsable`/`directeur`/`admin` mais pas pour `agent`
- [ ] Créer une chambre en tant que `directeur` fonctionne réellement (écrit bien dans Supabase — vérifiable via Table Editor sur supabase.com)

## 5. Après le déploiement — à faire sans tarder

- **Change les mots de passe de démo** (`admin`/`directeur`/`responsable`/`agent`) dès que le service est en ligne — ce sont des identifiants documentés dans ce dépôt, donc publics si le dépôt est public. Connecte-toi en `admin`, va dans Administration → Utilisateurs, modifie chaque compte.
- **Vérifie la visibilité du dépôt GitHub** (public vs privé). S'il est public, n'importe qui peut lire `DataInitializer.java` et donc les mots de passe de démo par défaut — encore une raison de les changer immédiatement après le premier déploiement.
- Le `JWT_SECRET` généré par Render est unique à cet environnement — ne le copie jamais dans un fichier versionné.

---

## 6. Jour d'ouverture de l'hôtel — check-list

À dérouler une fois, juste avant d'accueillir les premiers clients.

1. **Mots de passe** — le dépôt GitHub est public : les mots de passe d'origine des comptes `admin`, `directeur`, `responsable`, `agent` sont lisibles par tous. Connecte-toi en `admin`, **Administration → Utilisateurs** : crée un compte nominatif par employé, change le mot de passe des quatre comptes d'origine ou désactive ceux qui ne servent pas.
2. **Repartir d'une base propre** — si la base de production contient des essais (clients, séjours, paiements, mouvements de caisse de test), fais d'abord une sauvegarde (Supabase → Database → Backups), puis exécute **une seule fois** `scripts/remise-a-zero-avant-ouverture.sql` dans l'éditeur SQL de Supabase. Il efface toutes les opérations et conserve comptes, chambres, caisses (soldes à zéro), fournisseurs, carte du restaurant, stock et galerie. Ne jamais l'exécuter une fois l'hôtel en activité.
3. **Chambres et tarifs** — **Chambres** : vérifie numéros, types et tarifs (passage / nuitée), désactive celles qui n'existent pas.
4. **Comptes de versement** — **Caisse → Versements → Comptes** : renseigne le vrai numéro du compte bancaire et du compte Wave (un versement est refusé tant qu'ils sont vides).
5. **Stock** — les quantités d'origine sont des valeurs d'exemple : aligne chaque article sur la réserve réelle (un mouvement « Inventaire » ajoute, une « Perte » retire).
6. **Première vacation de caisse** — **Caisse → Ouvrir la vacation** : saisis le fond réellement compté dans le tiroir. Ensuite, à chaque ouverture, le montant proposé est le solde attendu : on ne le corrige que si le comptage diffère (l'écart est alors tracé dans le journal).
7. **Essai à blanc** — enregistre une arrivée, un paiement, télécharge le reçu PDF, enregistre le départ ; vérifie que tous les montants s'affichent en « FCFA ».

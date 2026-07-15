# Documentation — Résidence Hôtel GNIMAH

Application de gestion hôtelière (check-in, réservations, chambres, housekeeping, facturation, personnel, statistiques) pour la Résidence Hôtel GNIMAH (Abobo, Abidjan).

- **Frontend** : Angular 19 + PrimeNG 19, `gnimah-frontend/`
- **Backend** : Spring Boot 3.3.5 (Java 17), `gnimah-backend/`
- **Base de données** : PostgreSQL 17, migrations Flyway

---

## 1. Architecture

### 1.1 Vue d'ensemble

```
Navigateur (Angular SPA, port 4200)
        │  HTTP (JWT Bearer)
        ▼
Spring Boot API (port 8080, context-path /api)
        │  JPA / Hibernate
        ▼
PostgreSQL (gnimah_db, port 5432)
```

Toutes les routes de l'API sont préfixées par `/api` (`server.servlet.context-path`). L'authentification est stateless par JWT (`Authorization: Bearer <token>`), délivré par `POST /api/auth/login` et vérifié à chaque requête par `JwtAuthFilter`.

### 1.2 Frontend (`gnimah-frontend/src/app`)

```
core/
  models/       interfaces TypeScript miroir des DTO backend (Chambre, Client, Sejour, Reservation, Paiement, Evenement, TacheEntretien, Utilisateur, Kpi, Auth)
  services/     un service HTTP par ressource (ChambreService, ClientService, …) + PageHeaderService (titre de page dynamique de la topbar)
  guards/       AuthGuard — bloque les routes non authentifiées, et les routes marquées `data:{role:'DIRECTEUR'}` pour les agents
  interceptors/ AuthInterceptor (injecte le Bearer token), ErrorInterceptor (gestion centralisée des erreurs HTTP)
features/
  auth/login/                 page de connexion
  dashboard/                  tableau de bord (vue Directeur ET vue Agent, même composant, contenu conditionnel)
  sejours/check-in/           enregistrement d'un séjour (check-in)
  sejours/list/               séjours en cours + check-out
  reservations/list/, form/   liste des réservations + création
  chambres/plan/              plan visuel des chambres par étage (lecture/changement d'état rapide)
  chambres/list/              gestion complète des chambres (CRUD, filtres, activation/désactivation)
  entretien/                  housekeeping (kanban à faire / en cours / terminé)
  evenements/                 calendrier mensuel + liste des événements
  clients/list/                fiches clients (CRUD)
  paiements/                  liste des reçus + vue détaillée d'un reçu (impression / PDF)
  kpi/                        statistiques avancées avec filtre de période
  administration/utilisateurs/ gestion du personnel (CRUD + activation/désactivation)
shared/components/layout/     coquille de l'application (sidebar + topbar + <router-outlet>)
```

Chaque page définit son titre/sous-titre via `PageHeaderService.set(titre, sous-titre)` dans `ngOnInit` ; la topbar (dans `LayoutComponent`) s'y abonne et l'affiche — c'est ce qui évite de dupliquer un `<h1>` par page.

**Identité visuelle** : un système de classes utilitaires `.gn-*` (défini dans `src/styles.scss`) porte tout le rendu — cartes (`.gn-card`), boutons (`.gn-btn-p` / `.gn-btn-o`), badges de statut (`.gn-bdg`), avatars (`.gn-av`), grilles de mise en page (`.gn-kpi`, `.gn-dash2`, `.gn-dash3`, …). Les composants PrimeNG restants (dialogs, `p-select`, `p-datepicker`, `p-chart`) héritent d'une palette bordeaux via un preset PrimeNG personnalisé (`GnimahPreset` dans `app.module.ts`, généré avec `definePreset(Lara, …)`).

### 1.3 Backend (`gnimah-backend/src/main/java/com/gnimah/backend`)

```
controller/   un @RestController REST par ressource (routes, @PreAuthorize par rôle)
service/      logique métier (calculs de tarifs, changements d'état, agrégations KPI)
repository/   interfaces Spring Data JPA
entity/       entités JPA (une table = une classe, via Lombok @Builder/@Getter/@Setter)
dto/          objets d'entrée (*Request) et de sortie (*Response) — jamais l'entité JPA n'est exposée directement à l'API
security/     JwtAuthFilter, CustomUserDetailsService
config/       SecurityConfig (CORS + règles d'autorisation), DataInitializer (comptes de démo)
exception/    GlobalExceptionHandler (400/404/409 uniformes)
```

Convention : `Request` → `Service` (valide + construit l'entité) → `Repository.save()` → `Service.toResponse()` → `Response`. Toutes les méthodes de service sont `@Transactional`.

### 1.4 Sécurité & rôles

Trois rôles, croissants en droits : `AGENT` < `DIRECTEUR` < `ADMIN`.

| Rôle | Peut | Ne peut pas |
|---|---|---|
| **AGENT** | Check-in/out, réservations, chambres (lecture + changement d'état), clients, housekeeping, événements, reçus | Statistiques (`/kpi/**`), gestion du personnel, création/désactivation de chambres |
| **DIRECTEUR** | Tout ce que fait l'agent + Statistiques & KPI + créer/modifier/désactiver des chambres | Gestion du personnel |
| **ADMIN** | Tout, y compris la gestion des comptes utilisateurs | — |

Comptes de démonstration (créés par `DataInitializer` au premier démarrage) :

| Login | Mot de passe | Rôle |
|---|---|---|
| `admin` | `Admin@2026` | ADMIN |
| `directeur` | `Directeur@2026` | DIRECTEUR |
| `agent` | `Agent@2026` | AGENT |

---

## 2. Fonctionnement métier par module

### 2.1 Chambres (`/chambres/*`, page **Gestion des chambres**)

Une chambre a : numéro (unique), type (STANDARD/SUPÉRIEURE/DELUXE/SUITE/FAMILIALE), capacité, tarif passage (à l'heure) et tarif nuitée, étage, vue, équipements, description (visible client), observations (internes), état (LIBRE/OCCUPÉE/À_NETTOYER/EN_MAINTENANCE/HORS_SERVICE) et un indicateur `actif`.

- **Créer/modifier** : formulaire complet dans `chambres/list` (recherche par numéro/équipement, filtres par type/étage/statut).
- **Désactiver plutôt que supprimer** : une chambre est référencée par l'historique des séjours, réservations et tâches d'entretien (clés étrangères) — la supprimer casserait cet historique. Le bouton « Supprimer » bascule donc `actif=false` (`PATCH /chambres/{id}/actif`), ce qui la retire du plan des chambres et des sélections de check-in sans perdre son historique. Elle reste visible (grisée) dans l'écran de gestion (`GET /chambres/toutes`), et réactivable.
- **Changer l'état rapidement** : depuis le plan des chambres (`chambres/plan`), clic sur une chambre → `PATCH /chambres/{id}/etat`.
- Les **photos** ont une colonne dédiée (`photos`, texte) prête à recevoir une liste d'URLs — aucun pipeline d'upload n'est branché à ce jour (voir §5).

### 2.2 Clients (`/clients`, page **Fiches clients**)

Fiche client : civilité, nom/prénom, téléphone (recherche rapide), email, type et numéro de pièce d'identité (unique ensemble), nationalité, adresse. `nbSejours` et le badge **Fidèle** sont calculés côté backend à chaque check-in (`ClientService` incrémente le compteur ; au-delà d'un seuil, `clientFidele=true`).

Un client est recherché par téléphone ou n° de pièce (`GET /clients/by-telephone/{tel}`, `/clients/by-piece/{num}`) au moment du check-in, pour éviter les doublons.

### 2.3 Réservations (`/reservations`)

Une réservation précède un séjour : client + (chambre précise **ou** simplement un type de chambre souhaité) + dates d'arrivée/départ + acompte. Statuts : `EN_ATTENTE → CONFIRMÉE → ARRIVÉE`, ou `ANNULÉE` / `NO_SHOW`.

Le jour de l'arrivée, la réservation apparaît dans « Arrivées du jour » (dashboard agent) avec un bouton **Check-in** direct.

### 2.4 Séjours — Check-in / Check-out (`/sejours/check-in`, `/sejours`)

Le séjour est l'enregistrement central : il matérialise l'occupation réelle d'une chambre par un client.

1. **Rechercher ou créer le client** (téléphone / n° pièce).
2. **Choisir le mode** : Séjour (facturé à la nuitée) ou Passage (facturé à l'heure, non remboursable).
3. **Choisir une chambre disponible** (`GET /chambres/disponibles`).
4. **Renseigner les dates**, saisir l'acompte reçu → le reste à payer se calcule en temps réel.
5. **Valider** (`POST /sejours/check-in`) : le backend crée le séjour, calcule `montantTotal` selon le mode, marque la chambre `OCCUPÉE`, et génère le n° de reçu (`GN-xxxx`).
6. **Check-out** (`POST /sejours/{id}/check-out`, depuis `/sejours`) : clôture le séjour et libère la chambre.

### 2.5 Paiements & reçus (`/paiements`)

Chaque paiement est rattaché à un séjour (`sejour_id`). La liste (`Reçus & factures`) montre l'historique ; « Voir » ouvre le reçu détaillé (en-tête hôtel, lignes, totaux, reste à payer) avec impression navigateur et téléchargement PDF réel (`GET /paiements/{id}/recu`, généré avec iText).

### 2.6 Housekeeping / Entretien (`/entretien`)

Tâches de nettoyage/maintenance rattachées à une chambre : type (NETTOYAGE/MAINTENANCE/INSPECTION/RÉPARATION), priorité (Normale/Urgente), assigné à, statut (À_FAIRE → EN_COURS → TERMINÉ). Affiché en kanban 3 colonnes ; « Démarrer »/« Terminer » changent le statut (`PATCH /entretien/{id}/statut`).

### 2.7 Événements (`/evenements`)

Réservations de salles (séminaires, mariages, réunions) : titre, lieu, dates, nombre de participants, montant. Vue calendrier mensuel (`GET /evenements/calendrier?annee=&mois=`) + liste « à venir » avec édition/suppression.

### 2.8 Personnel (`/administration/utilisateurs`, ADMIN uniquement)

CRUD des comptes (nom, prénom, email, login, mot de passe, rôle). Comme pour les chambres, on **désactive** un compte plutôt que de le supprimer (`PATCH /utilisateurs/{id}/toggle-actif`) — un utilisateur désactivé ne peut plus se connecter mais reste l'auteur historique de ses séjours/tâches passés.

### 2.9 Statistiques & KPI (`/kpi`, DIRECTEUR/ADMIN uniquement)

Filtrable par période (par défaut : depuis le 1er du mois). Indicateurs hôteliers standards :

- **ADR** (Average Daily Rate) = revenus hébergement / nuitées vendues
- **RevPAR** (Revenue per Available Room) = revenus hébergement / chambres disponibles
- **TRevPAR** = revenus totaux (hébergement + à-côtés) / chambres disponibles
- **ALOS** (Average Length of Stay) = nuitées vendues / nombre de séjours
- Occupation, CA par jour, performance par agent (nb séjours + CA généré), taux de conversion réservations → séjours.

Le tableau de bord (`/dashboard`) reprend une sélection resserrée de ces indicateurs pour une lecture instantanée (voir §2.10), tandis que `/kpi` permet l'analyse détaillée sur une période choisie.

### 2.10 Tableau de bord (`/dashboard`)

Deux vues selon le rôle :

- **Directeur** : vue d'ensemble des chambres (total / disponibles / occupées / maintenance / taux d'occupation / clients hébergés), activité du jour (arrivées / départs / réservations à venir / reste à payer), revenus (jour + mois en cours), graphe CA 7 jours, donut d'occupation, indicateurs hôteliers, performance par agent.
- **Agent** : compteurs opérationnels (arrivées, départs, chambres à nettoyer, séjours en cours), liste des arrivées du jour avec check-in direct, liste « à traiter » (chambres à nettoyer, restes à payer, départs prévus) — construite dynamiquement à partir des vraies données (aucune donnée factice).

---

## 3. Base de données

### 3.1 Tables et rôle

| Table | Rôle | Relations |
|---|---|---|
| `utilisateurs` | Comptes du personnel (login, rôle) | référencée par `sejours.agent_id`, `reservations.agent_id`, `taches_entretien.agent_id/superviseur_id`, `evenements.agent_id`, `paiements.agent_id`, `depenses.agent_id`, `audit_logs.utilisateur_id` |
| `clients` | Fiches clients | référencée par `sejours.client_id`, `reservations.client_id`, `evenements.client_id` |
| `chambres` | Inventaire des chambres | référencée par `sejours.chambre_id`, `reservations.chambre_id`, `taches_entretien.chambre_id` |
| `sejours` | Occupations réelles (check-in → check-out) | → `clients`, `chambres`, `utilisateurs` ; référencée par `paiements.sejour_id`, `factures.sejour_id`, `depenses.sejour_id` |
| `reservations` | Réservations futures | → `clients`, `chambres` (optionnel), `utilisateurs` |
| `paiements` | Encaissements | → `sejours`, `utilisateurs` |
| `factures` | Reçus/factures PDF générés | → `sejours` |
| `taches_entretien` | Housekeeping / maintenance | → `chambres`, `utilisateurs` (×2 : agent + superviseur) |
| `evenements` | Location de salles | → `clients` (optionnel), `utilisateurs` |
| `depenses` | Dépenses d'exploitation | → `sejours` (optionnel), `utilisateurs` |
| `audit_logs` | Journal d'audit | → `utilisateurs` |

### 3.2 Schéma relationnel (simplifié)

```
utilisateurs ──┬──< sejours >──┬── clients
               │                └── chambres
               ├──< reservations >── clients / chambres
               ├──< taches_entretien >── chambres
               ├──< evenements >── clients
               └──< paiements/depenses >── sejours
```

### 3.3 Ordre d'alimentation (contraintes FK obligent)

1. `utilisateurs` (créé automatiquement par `DataInitializer`, ne pas insérer à la main)
2. `chambres`
3. `clients`
4. `reservations` (a besoin de `clients` + `chambres`)
5. `sejours` (a besoin de `clients` + `chambres`, éventuellement `reservations`)
6. `paiements` / `factures` / `depenses` (ont besoin de `sejours`)
7. `taches_entretien` (a besoin de `chambres`)
8. `evenements` (a besoin éventuellement de `clients`)

### 3.4 Exemple de données de démonstration

Les chambres de démo (12 chambres, étages 1-3, tous types) sont déjà injectées par `V2__initial_rooms.sql`. Exemple d'ajout manuel cohérent avec les contraintes :

```sql
-- Un client
INSERT INTO clients (civilite, nom, prenom, telephone, type_piece, numero_piece)
VALUES ('M', 'Konan', 'Yao', '+225 07 11 22 33', 'CNI', 'CI0011223');

-- Une réservation pour ce client, chambre 201
INSERT INTO reservations (numero_reservation, client_id, chambre_id, date_arrivee, date_depart, montant_prevu, acompte, statut)
VALUES ('RS-0001',
        (SELECT id FROM clients WHERE numero_piece = 'CI0011223'),
        (SELECT id FROM chambres WHERE numero = '201'),
        NOW() + INTERVAL '2 days', NOW() + INTERVAL '4 days', 60000, 20000, 'CONFIRMEE');
```

En pratique, on ne fait jamais cet INSERT à la main pour un séjour : le flux **Check-in** (`POST /sejours/check-in`) est le seul chemin qui calcule correctement `montant_total`/`reste_a_payer` et fait passer la chambre à `OCCUPÉE` — court-circuiter l'API en écrivant directement en base désynchronise l'état des chambres.

---

## 4. Scénario complet de bout en bout

Ce scénario suit exactement le cycle métier de l'application, du premier lancement à la lecture des statistiques.

1. **Connexion** — `/login`, compte `directeur` / `Directeur@2026`.
2. **Ajouter une chambre** — `Plan des chambres → Gestion des chambres → Nouvelle chambre` : numéro `401`, type Suite, vue Mer, tarif nuitée 45 000 XOF.
3. **Créer un client** — pendant le check-in (étape suivante) ou depuis `Fiches clients → Nouveau client` : Mme Fatou Diallo, +225 05 44 55 66, CNI CI0099887.
4. **Créer une réservation** (optionnel) — `Réservations → Nouvelle réservation` : Fatou Diallo, chambre 401, arrivée demain, 2 nuits, acompte 20 000 XOF. Statut `EN_ATTENTE`.
5. **Check-in** — le jour J, `Check-in / Séjour` : rechercher Fatou par téléphone (trouvée), mode Séjour, sélectionner la chambre 401, confirmer l'acompte → **Valider & générer le reçu**. La chambre 401 passe à `OCCUPÉE`, un reçu `GN-xxxx` est émis.
6. **Encaisser un complément** — depuis `Reçus & factures`, ouvrir le reçu, constater le reste à payer.
7. **Housekeeping** pendant le séjour — si besoin, créer une tâche d'entretien pour une autre chambre libérée (`Housekeeping → Nouvelle tâche`).
8. **Check-out** — à la date de départ, `Séjours en cours → Check-out` sur la ligne de Fatou : le séjour passe à `TERMINÉ`, la chambre 401 redevient disponible (à nettoyer selon le workflow).
9. **Statistiques** — `Statistiques & KPI`, filtrer sur la période du séjour : le CA, l'ADR, le RevPAR et la performance de l'agent qui a fait le check-in reflètent immédiatement l'opération.
10. **Tableau de bord** — de retour sur `/dashboard`, les compteurs (revenus du jour/mois, taux d'occupation, clients hébergés) intègrent la même opération en temps réel.

---

## 5. Points d'attention pour la suite

- **Environnement de build backend** : ce poste n'a que le JDK 25 sur le PATH, or Gradle 8.8 / Lombok (résolu par le BOM Spring Boot 3.3.5) ne le supportent pas. Deux ajustements ont été faits pour que `./gradlew` fonctionne ici : Lombok est épinglé en `1.18.36` dans `build.gradle`, et il faut lancer Gradle avec un JDK 17-21 (`export JAVA_HOME=<jdk17-ou-21>` avant `./gradlew`, ou configurer `org.gradle.java.home` localement — ce chemin n'a pas été committé car spécifique à cette machine).
- **Photos de chambre** : la colonne `photos` existe (chaîne de texte, URLs séparées par virgule) mais aucun pipeline d'upload/stockage n'est branché — à prévoir si la fonctionnalité doit devenir utilisable.
- **`ClientFormComponent`** (`features/clients/form/`) n'est raccordé à aucune route : la création/édition de client se fait via la boîte de dialogue intégrée à `Fiches clients`. Ce composant est mort ; à supprimer ou à re-brancher selon le besoin.
- **Suppression = désactivation** pour les chambres et le personnel (voir §2.1 et §2.8) : c'est un choix délibéré (intégrité de l'historique), pas un oubli.

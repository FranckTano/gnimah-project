-- ============================================================
-- V1 : Schéma initial - Résidence Hôtel GNIMAH
-- ============================================================

-- Utilisateurs (agents, directeurs, admins)
CREATE TABLE IF NOT EXISTS utilisateurs (
    id          BIGSERIAL PRIMARY KEY,
    nom         VARCHAR(100)  NOT NULL,
    prenom      VARCHAR(100)  NOT NULL,
    username    VARCHAR(50)   NOT NULL UNIQUE,
    email       VARCHAR(150)  UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    role        VARCHAR(20)   NOT NULL DEFAULT 'AGENT',
    actif       BOOLEAN       NOT NULL DEFAULT TRUE,
    telephone   VARCHAR(20),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    last_login  TIMESTAMP
);

-- Clients
CREATE TABLE IF NOT EXISTS clients (
    id              BIGSERIAL PRIMARY KEY,
    civilite        VARCHAR(10)   NOT NULL DEFAULT 'M',
    nom             VARCHAR(100)  NOT NULL,
    prenom          VARCHAR(100),
    telephone       VARCHAR(20)   NOT NULL,
    email           VARCHAR(150),
    type_piece      VARCHAR(20)   NOT NULL,
    numero_piece    VARCHAR(50)   NOT NULL,
    nationalite     VARCHAR(100)  DEFAULT 'Ivoirienne',
    adresse         TEXT,
    nb_sejours      INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE(type_piece, numero_piece)
);

-- Chambres
CREATE TABLE IF NOT EXISTS chambres (
    id              BIGSERIAL PRIMARY KEY,
    numero          VARCHAR(20)       NOT NULL UNIQUE,
    type            VARCHAR(20)       NOT NULL DEFAULT 'STANDARD',
    capacite        INT               NOT NULL DEFAULT 2,
    tarif_passage   DECIMAL(10,2)     NOT NULL,
    tarif_nuitee    DECIMAL(10,2)     NOT NULL,
    etat            VARCHAR(30)       NOT NULL DEFAULT 'LIBRE',
    etage           INT               DEFAULT 0,
    description     TEXT,
    equipements     TEXT,
    actif           BOOLEAN           NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP         NOT NULL DEFAULT NOW()
);

-- Séjours / Enregistrements
CREATE TABLE IF NOT EXISTS sejours (
    id              BIGSERIAL PRIMARY KEY,
    numero_recu     VARCHAR(50)   NOT NULL UNIQUE,
    client_id       BIGINT        NOT NULL REFERENCES clients(id),
    chambre_id      BIGINT        NOT NULL REFERENCES chambres(id),
    agent_id        BIGINT        REFERENCES utilisateurs(id),
    type_location   VARCHAR(20)   NOT NULL DEFAULT 'SEJOUR',
    date_entree     TIMESTAMP     NOT NULL,
    date_sortie     TIMESTAMP,
    heure_entree    TIME,
    heure_sortie    TIME,
    nb_jours        INT,
    nb_heures       INT,
    montant_total   DECIMAL(10,2) NOT NULL DEFAULT 0,
    montant_paye    DECIMAL(10,2) NOT NULL DEFAULT 0,
    reste_a_payer   DECIMAL(10,2) NOT NULL DEFAULT 0,
    statut          VARCHAR(30)   NOT NULL DEFAULT 'EN_COURS',
    notes           TEXT,
    reservation_id  BIGINT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Réservations
CREATE TABLE IF NOT EXISTS reservations (
    id                  BIGSERIAL PRIMARY KEY,
    numero_reservation  VARCHAR(50)   NOT NULL UNIQUE,
    client_id           BIGINT        NOT NULL REFERENCES clients(id),
    chambre_id          BIGINT        REFERENCES chambres(id),
    type_chambre        VARCHAR(20),
    agent_id            BIGINT        REFERENCES utilisateurs(id),
    date_arrivee        TIMESTAMP     NOT NULL,
    date_depart         TIMESTAMP     NOT NULL,
    nb_nuits            INT,
    montant_prevu       DECIMAL(10,2),
    acompte             DECIMAL(10,2) NOT NULL DEFAULT 0,
    statut              VARCHAR(30)   NOT NULL DEFAULT 'EN_ATTENTE',
    notes               TEXT,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Paiements
CREATE TABLE IF NOT EXISTS paiements (
    id                      BIGSERIAL PRIMARY KEY,
    sejour_id               BIGINT        NOT NULL REFERENCES sejours(id),
    montant                 DECIMAL(10,2) NOT NULL,
    mode                    VARCHAR(30)   NOT NULL DEFAULT 'ESPECES',
    reference_transaction   VARCHAR(100),
    date_paiement           TIMESTAMP     NOT NULL DEFAULT NOW(),
    agent_id                BIGINT        REFERENCES utilisateurs(id),
    notes                   TEXT
);

-- Factures / Reçus
CREATE TABLE IF NOT EXISTS factures (
    id              BIGSERIAL PRIMARY KEY,
    numero          VARCHAR(50)   NOT NULL UNIQUE,
    sejour_id       BIGINT        NOT NULL REFERENCES sejours(id),
    montant_total   DECIMAL(10,2) NOT NULL,
    montant_paye    DECIMAL(10,2) NOT NULL DEFAULT 0,
    reste_a_payer   DECIMAL(10,2) NOT NULL DEFAULT 0,
    pdf_path        VARCHAR(500),
    date_emission   TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Tâches d'entretien
CREATE TABLE IF NOT EXISTS taches_entretien (
    id              BIGSERIAL PRIMARY KEY,
    chambre_id      BIGINT        NOT NULL REFERENCES chambres(id),
    type_tache      VARCHAR(50)   NOT NULL DEFAULT 'NETTOYAGE',
    description     TEXT,
    statut          VARCHAR(20)   NOT NULL DEFAULT 'A_FAIRE',
    priorite        INT           NOT NULL DEFAULT 2,
    agent_id        BIGINT        REFERENCES utilisateurs(id),
    superviseur_id  BIGINT        REFERENCES utilisateurs(id),
    date_debut      TIMESTAMP,
    date_fin        TIMESTAMP,
    date_limite     TIMESTAMP,
    notes           TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Événements
CREATE TABLE IF NOT EXISTS evenements (
    id              BIGSERIAL PRIMARY KEY,
    intitule        VARCHAR(200)  NOT NULL,
    client_id       BIGINT        REFERENCES clients(id),
    type_evenement  VARCHAR(50)   DEFAULT 'REUNION',
    salle           VARCHAR(100),
    date_debut      TIMESTAMP     NOT NULL,
    date_fin        TIMESTAMP,
    nb_personnes    INT           DEFAULT 1,
    montant         DECIMAL(10,2) DEFAULT 0,
    statut          VARCHAR(20)   NOT NULL DEFAULT 'PLANIFIE',
    description     TEXT,
    agent_id        BIGINT        REFERENCES utilisateurs(id),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Dépenses
CREATE TABLE IF NOT EXISTS depenses (
    id              BIGSERIAL PRIMARY KEY,
    libelle         VARCHAR(200)  NOT NULL,
    montant         DECIMAL(10,2) NOT NULL,
    categorie       VARCHAR(100)  NOT NULL DEFAULT 'DIVERS',
    date_depense    DATE          NOT NULL DEFAULT CURRENT_DATE,
    sejour_id       BIGINT        REFERENCES sejours(id),
    agent_id        BIGINT        REFERENCES utilisateurs(id),
    notes           TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Journal d'audit
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    utilisateur_id  BIGINT        REFERENCES utilisateurs(id),
    username        VARCHAR(50),
    action          VARCHAR(100)  NOT NULL,
    entite          VARCHAR(100),
    entite_id       BIGINT,
    details         TEXT,
    ip_address      VARCHAR(50),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Indexes pour les performances
CREATE INDEX IF NOT EXISTS idx_clients_telephone   ON clients(telephone);
CREATE INDEX IF NOT EXISTS idx_clients_numero_piece ON clients(numero_piece);
CREATE INDEX IF NOT EXISTS idx_clients_nom         ON clients(nom);
CREATE INDEX IF NOT EXISTS idx_sejours_client      ON sejours(client_id);
CREATE INDEX IF NOT EXISTS idx_sejours_chambre     ON sejours(chambre_id);
CREATE INDEX IF NOT EXISTS idx_sejours_date_entree ON sejours(date_entree);
CREATE INDEX IF NOT EXISTS idx_sejours_statut      ON sejours(statut);
CREATE INDEX IF NOT EXISTS idx_reservations_client ON reservations(client_id);
CREATE INDEX IF NOT EXISTS idx_reservations_chambre ON reservations(chambre_id);
CREATE INDEX IF NOT EXISTS idx_reservations_dates  ON reservations(date_arrivee, date_depart);
CREATE INDEX IF NOT EXISTS idx_paiements_sejour    ON paiements(sejour_id);
CREATE INDEX IF NOT EXISTS idx_taches_chambre      ON taches_entretien(chambre_id);
CREATE INDEX IF NOT EXISTS idx_audit_created       ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_utilisateur   ON audit_logs(utilisateur_id);
CREATE INDEX IF NOT EXISTS idx_depenses_date       ON depenses(date_depense);

-- Demandes de réinitialisation de mot de passe (file d'attente validée par un Administrateur)
CREATE TABLE IF NOT EXISTS password_reset_requests (
    id              BIGSERIAL PRIMARY KEY,
    utilisateur_id  BIGINT        NOT NULL REFERENCES utilisateurs(id),
    statut          VARCHAR(20)   NOT NULL DEFAULT 'EN_ATTENTE',
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    traite_par_id   BIGINT        REFERENCES utilisateurs(id),
    traite_le       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_statut ON password_reset_requests(statut);

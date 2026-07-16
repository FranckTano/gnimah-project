-- Centre de notifications (partagé entre tout le personnel connecté)
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(40)   NOT NULL,
    titre       VARCHAR(200)  NOT NULL,
    message     TEXT,
    lien        VARCHAR(200),
    lu          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_lu ON notifications(lu);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at);

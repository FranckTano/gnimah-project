-- Galerie de la résidence, pilotée par l'administrateur depuis l'application.
--
-- Jusqu'ici le catalogue de photos vivait en dur dans le frontend : ajouter un
-- visuel imposait une modification de code et un redéploiement. Cette table le
-- rend administrable.
--
-- Seules les métadonnées sont stockées ; les fichiers vivent sur Cloudinary.
-- `public_id` conserve l'identifiant Cloudinary pour pouvoir supprimer l'objet
-- distant en même temps que la ligne.
CREATE TABLE IF NOT EXISTS medias_residence (
    id              BIGSERIAL PRIMARY KEY,
    titre           VARCHAR(150) NOT NULL,
    categorie       VARCHAR(60)  NOT NULL DEFAULT 'Chambres',
    url             TEXT         NOT NULL,
    url_vignette    TEXT,
    public_id       VARCHAR(255),
    -- Ordre d'affichage dans la galerie ; les plus petits d'abord.
    position        INTEGER      NOT NULL DEFAULT 0,
    -- Masquer un visuel sans le supprimer (ex : chambre en travaux).
    actif           BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Marque le visuel utilisé en bandeau d'accueil et comme affiche.
    mis_en_avant    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

-- La galerie se lit toujours triée et filtrée sur l'actif.
CREATE INDEX IF NOT EXISTS idx_medias_residence_ordre
    ON medias_residence (actif, position, id);

-- Un même fichier Cloudinary ne doit pas être référencé deux fois : l'index
-- partiel laisse cohabiter plusieurs lignes sans public_id (URL externes
-- saisies à la main), tout en interdisant les doublons d'objets distants.
CREATE UNIQUE INDEX IF NOT EXISTS uq_medias_residence_public_id
    ON medias_residence (public_id) WHERE public_id IS NOT NULL;

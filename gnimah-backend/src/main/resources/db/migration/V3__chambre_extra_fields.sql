-- Champs additionnels pour la fiche chambre (vue, observations internes, photos)
ALTER TABLE chambres ADD COLUMN IF NOT EXISTS vue VARCHAR(30);
ALTER TABLE chambres ADD COLUMN IF NOT EXISTS observations TEXT;
ALTER TABLE chambres ADD COLUMN IF NOT EXISTS photos TEXT;

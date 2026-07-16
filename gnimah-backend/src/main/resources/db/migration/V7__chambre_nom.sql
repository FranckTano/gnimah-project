-- Nom personnalisable de la chambre (ex: "Suite Présidentielle"), en plus du numéro
ALTER TABLE chambres ADD COLUMN IF NOT EXISTS nom VARCHAR(150);

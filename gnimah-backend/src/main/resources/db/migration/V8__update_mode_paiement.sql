-- Migration V8 : Mise à jour des modes de paiement
-- Renommer CARTE → CARTE_BANCAIRE pour cohérence avec l'enum Java
UPDATE paiements SET mode = 'CARTE_BANCAIRE' WHERE mode = 'CARTE';

-- Agrandir la colonne mode pour accueillir les nouvelles valeurs (ex: CARTE_BANCAIRE = 14 chars)
ALTER TABLE paiements ALTER COLUMN mode TYPE VARCHAR(30);

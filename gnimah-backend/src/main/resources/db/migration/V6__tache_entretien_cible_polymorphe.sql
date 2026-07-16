-- Une tâche d'entretien peut désormais cibler une chambre, une salle (texte libre) ou un événement,
-- ou aucune cible précise (intervention générale). Ajout d'un titre ; l'échéance (date + heure)
-- utilise la colonne date_limite (TIMESTAMP) déjà existante, pas de colonne heure séparée.
ALTER TABLE taches_entretien ALTER COLUMN chambre_id DROP NOT NULL;
ALTER TABLE taches_entretien ADD COLUMN IF NOT EXISTS titre VARCHAR(200);
ALTER TABLE taches_entretien ADD COLUMN IF NOT EXISTS salle VARCHAR(150);
ALTER TABLE taches_entretien ADD COLUMN IF NOT EXISTS evenement_id BIGINT REFERENCES evenements(id);

-- Titre par défaut pour les tâches existantes (colonne ajoutée après coup)
UPDATE taches_entretien SET titre = COALESCE(type_tache, 'Tâche') WHERE titre IS NULL;

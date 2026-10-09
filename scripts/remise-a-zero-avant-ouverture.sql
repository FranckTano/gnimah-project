-- ============================================================================
-- GNIMAH — Remise à zéro des données d'exploitation AVANT L'OUVERTURE
-- ============================================================================
--
-- À exécuter UNE SEULE FOIS, dans l'éditeur SQL de Supabase, juste avant la
-- mise en service réelle de la résidence. JAMAIS une fois l'hôtel en activité :
-- toutes les opérations enregistrées seraient définitivement effacées.
--
-- Avant de l'exécuter : EXPORTER LA BASE. L'offre gratuite de Supabase ne fait
-- aucune sauvegarde automatique ; depuis un poste où PostgreSQL est installé :
--   pg_dump "postgresql://postgres.<ref>:<mot-de-passe>@<hôte-pooler>:5432/postgres" -Fc -f gnimah-avant-ouverture.dump
--
-- EFFACE  : clients, réservations, séjours, paiements et reçus, événements,
--           tâches d'entretien, incidents, additions du restaurant, vacations
--           et mouvements de caisse, versements, dépenses, mouvements de stock,
--           notifications, journal d'audit, demandes de réinitialisation.
--
-- CONSERVE : comptes utilisateurs, chambres (toutes remises « libres »),
--           caisses et comptes de versement (soldes remis à zéro),
--           fournisseurs, carte du restaurant, articles de stock (quantités à
--           recaler ensuite par un mouvement d'inventaire), galerie de photos.
--
-- Pas de CASCADE : si une table conservée référençait une table vidée, la
-- commande échouerait au lieu d'effacer silencieusement autre chose.
-- ============================================================================

BEGIN;

TRUNCATE TABLE
    actions_versement, versements,
    mouvements_caisse, sessions_caisse,
    lignes_commande, commandes_restaurant,
    mouvements_stock, incidents_chambre, depenses,
    paiements, factures, taches_entretien,
    sejours, reservations, evenements, clients,
    notifications, audit_logs, password_reset_requests
RESTART IDENTITY;

UPDATE chambres SET etat = 'LIBRE';
UPDATE caisses SET solde = 0;
UPDATE comptes_financiers SET solde = 0;

COMMIT;

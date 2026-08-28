-- Amorce la galerie avec les visuels déjà en ligne sur Cloudinary.
--
-- Sans cela, la page « La Résidence » serait vide au premier démarrage après la
-- migration : le catalogue vivait jusqu'ici en dur dans le frontend, et rien ne
-- l'aurait repris. L'administrateur part donc d'une galerie complète, qu'il peut
-- ensuite compléter ou élaguer depuis l'application.
--
-- `f_auto,q_auto` laisse Cloudinary livrer du WebP/AVIF selon le navigateur.
-- `ON CONFLICT DO NOTHING` rend la migration rejouable sans créer de doublons.

INSERT INTO medias_residence (titre, categorie, url, url_vignette, public_id, position, actif, mis_en_avant, created_at, updated_at)
SELECT v.titre, v.categorie,
       'https://res.cloudinary.com/dehzeeo6p/image/upload/f_auto,q_auto/gnimah/photos/' || v.slug || '.jpg',
       'https://res.cloudinary.com/dehzeeo6p/image/upload/f_auto,q_auto/gnimah/photos/' || v.slug || '-sm.jpg',
       'gnimah/photos/' || v.slug,
       v.position, TRUE, v.position = 0, NOW(), NOW()
FROM (VALUES
    ('facade-patio',           'Patio & allée centrale',       'Extérieur',       0),
    ('suite-lit-king',         'Suite — lit king size',        'Chambres',        1),
    ('chambre-standard',       'Chambre standard climatisée',  'Chambres',        2),
    ('chambre-confort',        'Chambre confort',              'Chambres',        3),
    ('chambre-bureau',         'Chambre avec coin bureau',     'Chambres',        4),
    ('chambre-fresque-ciel',   'Chambre — fresque ciel',       'Chambres',        5),
    ('chambre-salon-tv',       'Coin salon & télévision',      'Chambres',        6),
    ('chambre-tv-fresque',     'Chambre — écran plat',         'Chambres',        7),
    ('chambre-coin-detente',   'Coin détente & minibar',       'Chambres',        8),
    ('restaurant-salle',       'Salle de restauration',        'Restauration',    9),
    ('salon-lounge',           'Salon lounge',                 'Espaces communs', 10),
    ('couloir-etage',          'Couloir des chambres',         'Espaces communs', 11),
    ('salle-de-bain',          'Salle de bain privative',      'Équipements',     12),
    ('equipement-chauffe-eau', 'Eau chaude en continu',        'Équipements',     13)
) AS v(slug, titre, categorie, position)
-- L'index d'unicité de public_id est partiel (WHERE public_id IS NOT NULL) :
-- PostgreSQL exige que la clause ON CONFLICT reprenne le même prédicat, sinon
-- il ne peut pas rattacher la cible à un index et rejette la requête.
ON CONFLICT (public_id) WHERE public_id IS NOT NULL DO NOTHING;

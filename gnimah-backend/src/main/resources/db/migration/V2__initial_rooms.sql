-- V2 : Données initiales - Chambres
-- Les utilisateurs sont créés via DataInitializer.java (BCrypt dynamique)

INSERT INTO chambres (numero, type, capacite, tarif_passage, tarif_nuitee, etat, etage, description, equipements)
VALUES
    ('101', 'STANDARD',   2, 5000,  15000, 'LIBRE', 1, 'Chambre standard vue sur cour',     'Climatisation,TV,WiFi,Salle de bain'),
    ('102', 'STANDARD',   2, 5000,  15000, 'LIBRE', 1, 'Chambre standard',                  'Climatisation,TV,WiFi,Salle de bain'),
    ('103', 'STANDARD',   2, 5000,  15000, 'LIBRE', 1, 'Chambre standard',                  'Climatisation,TV,WiFi,Salle de bain'),
    ('104', 'SUPERIEURE', 2, 7000,  20000, 'LIBRE', 1, 'Chambre supérieure',                'Climatisation,TV,WiFi,Salle de bain,Minibar'),
    ('105', 'SUPERIEURE', 2, 7000,  20000, 'LIBRE', 1, 'Chambre supérieure',                'Climatisation,TV,WiFi,Salle de bain,Minibar'),
    ('201', 'DELUXE',     3, 10000, 30000, 'LIBRE', 2, 'Chambre deluxe avec balcon',        'Climatisation,TV,WiFi,Salle de bain,Minibar,Balcon'),
    ('202', 'DELUXE',     3, 10000, 30000, 'LIBRE', 2, 'Chambre deluxe',                    'Climatisation,TV,WiFi,Salle de bain,Minibar'),
    ('203', 'SUITE',      4, 15000, 50000, 'LIBRE', 2, 'Suite avec salon',                  'Climatisation,TV,WiFi,Salle de bain,Minibar,Salon,Balcon'),
    ('204', 'FAMILIALE',  5, 12000, 40000, 'LIBRE', 2, 'Chambre familiale',                 'Climatisation,TV,WiFi,2 Salles de bain'),
    ('301', 'STANDARD',   2, 5000,  15000, 'LIBRE', 3, 'Chambre standard vue panoramique',  'Climatisation,TV,WiFi,Salle de bain'),
    ('302', 'STANDARD',   2, 5000,  15000, 'LIBRE', 3, 'Chambre standard',                  'Climatisation,TV,WiFi,Salle de bain'),
    ('303', 'SUPERIEURE', 2, 7000,  20000, 'LIBRE', 3, 'Chambre supérieure',                'Climatisation,TV,WiFi,Salle de bain,Minibar')
ON CONFLICT (numero) DO NOTHING;

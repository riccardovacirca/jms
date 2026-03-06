-- ============================================================================
-- SEED: MODULO CONTATTI
-- ============================================================================

INSERT INTO contatti (nome, cognome, telefono, email, citta, provincia, stato, consenso, blacklist, created_at) VALUES
    ('Mario',     'Rossi',     '3331234501', 'mario.rossi@example.com',       'Milano',  'MI', 1, TRUE,  FALSE, '2026-01-10 09:00:00'),
    ('Laura',     'Bianchi',   '3331234502', 'laura.bianchi@example.com',     'Roma',    'RM', 1, TRUE,  FALSE, '2026-01-11 10:00:00'),
    ('Giuseppe',  'Verdi',     '3331234503', 'giuseppe.verdi@example.com',    'Torino',  'TO', 1, TRUE,  FALSE, '2026-01-12 11:00:00'),
    ('Anna',      'Ferrari',   '3331234504', 'anna.ferrari@example.com',      'Bologna', 'BO', 1, FALSE, FALSE, '2026-01-13 09:30:00'),
    ('Marco',     'Esposito',  '3331234505', 'marco.esposito@example.com',    'Napoli',  'NA', 1, TRUE,  FALSE, '2026-01-14 14:00:00'),
    ('Chiara',    'Romano',    '3331234506', 'chiara.romano@example.com',     'Firenze', 'FI', 2, FALSE, FALSE, '2026-01-15 10:00:00'),
    ('Luca',      'Moretti',   '3331234507', 'luca.moretti@example.com',      'Venezia', 'VE', 1, TRUE,  FALSE, '2026-01-16 09:00:00'),
    ('Sofia',     'Ricci',     '3331234508', 'sofia.ricci@example.com',       'Genova',  'GE', 1, TRUE,  FALSE, '2026-01-17 11:00:00'),
    ('Davide',    'Conti',     '3331234509', 'davide.conti@example.com',      'Palermo', 'PA', 3, FALSE, TRUE,  '2026-01-18 09:00:00'),
    ('Elena',     'Mancini',   '3331234510', 'elena.mancini@example.com',     'Bari',    'BA', 1, TRUE,  FALSE, '2026-01-19 10:00:00');

-- ============================================================================
-- SEED: LISTE
-- ============================================================================

INSERT INTO liste (nome, descrizione, consenso, stato, created_at) VALUES
    ('Lead Freddi 2026',  'Contatti da lavorare nel primo semestre 2026', FALSE, 1, '2026-01-20 09:00:00'),
    ('Clienti Premium',   'Clienti con alto potenziale di acquisto',      TRUE,  1, '2026-01-21 09:00:00'),
    ('Prospect Marzo',    'Nuovi prospect acquisiti a marzo',             FALSE, 1, '2026-02-01 09:00:00');

-- ============================================================================
-- SEED: ASSOCIAZIONI LISTA-CONTATTI
-- ============================================================================

-- Lista 1: Lead Freddi 2026 → contatti 1,2,3,4,5
INSERT INTO lista_contatti (lista_id, contatto_id, created_at) VALUES
    (1, 1, '2026-01-22 09:00:00'),
    (1, 2, '2026-01-22 09:01:00'),
    (1, 3, '2026-01-22 09:02:00'),
    (1, 4, '2026-01-22 09:03:00'),
    (1, 5, '2026-01-22 09:04:00');

-- Lista 2: Clienti Premium → contatti 2,4,6,8
INSERT INTO lista_contatti (lista_id, contatto_id, created_at) VALUES
    (2, 2,  '2026-01-23 09:00:00'),
    (2, 4,  '2026-01-23 09:01:00'),
    (2, 6,  '2026-01-23 09:02:00'),
    (2, 8,  '2026-01-23 09:03:00');

-- Lista 3: Prospect Marzo → contatti 1,7,8,10
INSERT INTO lista_contatti (lista_id, contatto_id, created_at) VALUES
    (3, 1,  '2026-02-02 09:00:00'),
    (3, 7,  '2026-02-02 09:01:00'),
    (3, 8,  '2026-02-02 09:02:00'),
    (3, 10, '2026-02-02 09:03:00');

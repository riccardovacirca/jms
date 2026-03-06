-- ============================================================================
-- SEED: AGENTI
-- ============================================================================

INSERT INTO agenti (nome, cognome, email, telefono, note, attivo, created_at) VALUES
    ('Giovanni', 'Conti',   'giovanni.conti@azienda.it',   '3339001001', NULL,                          1, '2026-01-05 09:00:00'),
    ('Francesca','Marino',  'francesca.marino@azienda.it', '3339001002', NULL,                          1, '2026-01-05 09:00:00'),
    ('Roberto',  'Galli',   'roberto.galli@azienda.it',    '3339001003', 'Agente non più operativo',    0, '2026-01-05 09:00:00');

-- ============================================================================
-- SEED: DISPONIBILITA SETTIMANALI
-- ============================================================================

-- Giovanni Conti (id=1): lun mattina, lun pomeriggio, mer mattina, ven intera
INSERT INTO agenti_disponibilita (agente_id, giorno_settimana, ora_inizio, ora_fine, created_at) VALUES
    (1, 1, '09:00:00', '12:00:00', '2026-01-06 09:00:00'),
    (1, 1, '14:00:00', '18:00:00', '2026-01-06 09:00:00'),
    (1, 3, '09:00:00', '13:00:00', '2026-01-06 09:00:00'),
    (1, 5, '09:00:00', '17:00:00', '2026-01-06 09:00:00');

-- Francesca Marino (id=2): mar mattina, mer pomeriggio, gio intera
INSERT INTO agenti_disponibilita (agente_id, giorno_settimana, ora_inizio, ora_fine, created_at) VALUES
    (2, 2, '08:00:00', '12:00:00', '2026-01-06 09:00:00'),
    (2, 3, '14:00:00', '18:00:00', '2026-01-06 09:00:00'),
    (2, 4, '09:00:00', '17:00:00', '2026-01-06 09:00:00');

-- Roberto Galli (id=3): lun intera — inattivo, disponibilità storica
INSERT INTO agenti_disponibilita (agente_id, giorno_settimana, ora_inizio, ora_fine, created_at) VALUES
    (3, 1, '09:00:00', '18:00:00', '2026-01-06 09:00:00');

-- ============================================================================
-- SEED: APPUNTAMENTI
-- ============================================================================

-- Giovanni Conti con Mario Rossi (contatto_id=1)
INSERT INTO agenti_appuntamenti (agente_id, contatto_id, data_ora, durata_minuti, note, stato, created_at) VALUES
    (1, 1, '2026-03-03 10:00:00', 60, 'Presentazione offerta iniziale',    'PROGRAMMATO', '2026-02-14 09:00:00'),
    (1, 3, '2026-03-05 09:00:00', 30, 'Verifica interesse prodotto',       'PROGRAMMATO', '2026-02-14 09:00:00'),
    (1, 5, '2026-02-17 14:00:00', 45, 'Follow-up telefonico',              'COMPLETATO',  '2026-02-10 09:00:00');

-- Francesca Marino con Laura Bianchi (contatto_id=2) e Anna Ferrari (contatto_id=4)
INSERT INTO agenti_appuntamenti (agente_id, contatto_id, data_ora, durata_minuti, note, stato, created_at) VALUES
    (2, 2, '2026-03-04 08:00:00', 60, 'Demo prodotto',                     'PROGRAMMATO', '2026-02-14 09:00:00'),
    (2, 4, '2026-03-06 09:00:00', 30, 'Raccolta requisiti',                'PROGRAMMATO', '2026-02-14 09:00:00'),
    (2, 8, '2026-02-11 14:00:00', 30, 'Chiamata conoscitiva',              'ANNULLATO',   '2026-02-10 09:00:00');

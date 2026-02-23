-- ============================================================================
-- SEED: CAMPAGNE
-- ============================================================================

INSERT INTO campagne (nome, descrizione, tipo, stato, data_inizio, data_fine, created_at) VALUES
    ('Campagna Primavera 2026', 'Campagna outbound per il primo trimestre 2026',    'outbound', 1, '2026-03-01', '2026-05-31', '2026-02-01 09:00:00'),
    ('Rinnovi Q1 2026',         'Campagna per rinnovo contratti in scadenza Q1',    'outbound', 1, '2026-01-01', '2026-03-31', '2026-01-05 09:00:00'),
    ('Test Inbound Marzo',      'Campagna inbound di test per il mese di marzo',    'inbound',  0, '2026-03-01', '2026-03-31', '2026-02-10 09:00:00');

-- ============================================================================
-- SEED: ASSOCIAZIONI CAMPAGNA-LISTE
-- ============================================================================

-- Campagna 1: Primavera → Liste 1 e 3
INSERT INTO campagna_liste (campagna_id, lista_id, created_at) VALUES
    (1, 1, '2026-02-01 10:00:00'),
    (1, 3, '2026-02-01 10:01:00');

-- Campagna 2: Rinnovi → Lista 2
INSERT INTO campagna_liste (campagna_id, lista_id, created_at) VALUES
    (2, 2, '2026-01-05 10:00:00');

-- Campagna 3: Test → Lista 3
INSERT INTO campagna_liste (campagna_id, lista_id, created_at) VALUES
    (3, 3, '2026-02-10 10:00:00');

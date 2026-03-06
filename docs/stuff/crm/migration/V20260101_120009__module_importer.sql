-- ============================================================================
-- MODULO IMPORTER
-- ============================================================================

-- Tabella sessioni di import
CREATE TABLE IF NOT EXISTS import_sessions (
    id VARCHAR(36) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    row_count INTEGER NOT NULL,
    headers TEXT,
    preview TEXT,
    column_mapping TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Tabella dizionario campi contatti
CREATE TABLE IF NOT EXISTS contatti_campo_dizionario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome_campo VARCHAR(50) NOT NULL UNIQUE,
    etichetta VARCHAR(100) NOT NULL,
    descrizione TEXT,
    tipo_dato VARCHAR(20),
    obbligatorio BOOLEAN DEFAULT FALSE,
    ordine INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_import_sessions_status ON import_sessions(status);
CREATE INDEX IF NOT EXISTS idx_campo_dizionario_nome ON contatti_campo_dizionario(nome_campo);

-- Popolamento dizionario campi
INSERT INTO contatti_campo_dizionario (nome_campo, etichetta, descrizione, tipo_dato, obbligatorio, ordine, created_at) VALUES
    ('nome', 'Nome', 'Nome del contatto', 'text', 0, 1, CURRENT_TIMESTAMP),
    ('cognome', 'Cognome', 'Cognome del contatto', 'text', 0, 2, CURRENT_TIMESTAMP),
    ('ragione_sociale', 'Ragione Sociale', 'Ragione sociale azienda', 'text', 0, 3, CURRENT_TIMESTAMP),
    ('telefono', 'Telefono', 'Numero di telefono', 'phone', 0, 4, CURRENT_TIMESTAMP),
    ('email', 'Email', 'Indirizzo email', 'email', 0, 5, CURRENT_TIMESTAMP),
    ('indirizzo', 'Indirizzo', 'Indirizzo completo', 'text', 0, 6, CURRENT_TIMESTAMP),
    ('citta', 'Città', 'Città', 'text', 0, 7, CURRENT_TIMESTAMP),
    ('cap', 'CAP', 'Codice postale', 'text', 0, 8, CURRENT_TIMESTAMP),
    ('provincia', 'Provincia', 'Provincia', 'text', 0, 9, CURRENT_TIMESTAMP),
    ('note', 'Note', 'Note', 'textarea', 0, 10, CURRENT_TIMESTAMP);

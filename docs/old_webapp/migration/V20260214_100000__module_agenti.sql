-- ============================================================================
-- MODULO AGENTI
-- ============================================================================

CREATE TABLE IF NOT EXISTS agenti (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(100),
    cognome VARCHAR(100),
    email VARCHAR(100),
    telefono VARCHAR(20),
    note TEXT,
    attivo INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agenti_attivo ON agenti(attivo);
CREATE INDEX IF NOT EXISTS idx_agenti_cognome ON agenti(cognome);

-- ============================================================================
-- DISPONIBILITA SETTIMANALE AGENTI
-- ============================================================================

CREATE TABLE IF NOT EXISTS agenti_disponibilita (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    agente_id INTEGER NOT NULL,
    giorno_settimana INTEGER NOT NULL,
    ora_inizio TIME NOT NULL,
    ora_fine TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agente_id) REFERENCES agenti(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_agenti_disponibilita_agente ON agenti_disponibilita(agente_id);
CREATE INDEX IF NOT EXISTS idx_agenti_disponibilita_giorno ON agenti_disponibilita(giorno_settimana);

-- ============================================================================
-- APPUNTAMENTI AGENTI
-- ============================================================================

CREATE TABLE IF NOT EXISTS agenti_appuntamenti (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    agente_id INTEGER NOT NULL,
    contatto_id INTEGER,
    data_ora TIMESTAMP NOT NULL,
    durata_minuti INTEGER NOT NULL DEFAULT 30,
    note TEXT,
    stato VARCHAR(50) NOT NULL DEFAULT 'PROGRAMMATO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (agente_id) REFERENCES agenti(id) ON DELETE CASCADE,
    FOREIGN KEY (contatto_id) REFERENCES contatti(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_agenti_appuntamenti_agente ON agenti_appuntamenti(agente_id);
CREATE INDEX IF NOT EXISTS idx_agenti_appuntamenti_data ON agenti_appuntamenti(data_ora);
CREATE INDEX IF NOT EXISTS idx_agenti_appuntamenti_contatto ON agenti_appuntamenti(contatto_id);
CREATE INDEX IF NOT EXISTS idx_agenti_appuntamenti_stato ON agenti_appuntamenti(stato);

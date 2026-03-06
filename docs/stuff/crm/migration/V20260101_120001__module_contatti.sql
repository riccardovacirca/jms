-- ============================================================================
-- MODULO CONTATTI
-- ============================================================================

CREATE TABLE IF NOT EXISTS contatti (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(100),
    cognome VARCHAR(100),
    ragione_sociale VARCHAR(200),
    telefono VARCHAR(20),
    email VARCHAR(100),
    indirizzo TEXT,
    citta VARCHAR(100),
    cap VARCHAR(10),
    provincia VARCHAR(50),
    note TEXT,
    stato INTEGER DEFAULT 1,
    consenso BOOLEAN DEFAULT FALSE,
    blacklist BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contatti_telefono ON contatti(telefono);
CREATE INDEX IF NOT EXISTS idx_contatti_email ON contatti(email);
CREATE INDEX IF NOT EXISTS idx_contatti_stato ON contatti(stato);
CREATE INDEX IF NOT EXISTS idx_contatti_blacklist ON contatti(blacklist);
CREATE INDEX IF NOT EXISTS idx_contatti_cognome ON contatti(cognome);

-- ============================================================================
-- MODULO LISTE
-- ============================================================================

CREATE TABLE IF NOT EXISTS liste (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descrizione TEXT,
    consenso BOOLEAN DEFAULT FALSE,
    stato INTEGER DEFAULT 1,
    scadenza DATE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_liste_stato ON liste(stato);
CREATE INDEX IF NOT EXISTS idx_liste_scadenza ON liste(scadenza);
CREATE INDEX IF NOT EXISTS idx_liste_deleted_at ON liste(deleted_at);

-- ============================================================================
-- RELAZIONE LISTE-CONTATTI (Many-to-Many)
-- ============================================================================

CREATE TABLE IF NOT EXISTS lista_contatti (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    lista_id INTEGER NOT NULL,
    contatto_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lista_id) REFERENCES liste(id) ON DELETE CASCADE,
    FOREIGN KEY (contatto_id) REFERENCES contatti(id) ON DELETE CASCADE,
    UNIQUE(lista_id, contatto_id)
);

CREATE INDEX IF NOT EXISTS idx_lista_contatti_lista ON lista_contatti(lista_id);
CREATE INDEX IF NOT EXISTS idx_lista_contatti_contatto ON lista_contatti(contatto_id);

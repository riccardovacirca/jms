-- ============================================================================
-- MODULO CONTATTI
-- ============================================================================

CREATE TABLE contatti (
    id              SERIAL PRIMARY KEY,
    nome            VARCHAR(100),
    cognome         VARCHAR(100),
    ragione_sociale VARCHAR(200),
    telefono        VARCHAR(20),
    email           VARCHAR(100),
    indirizzo       TEXT,
    citta           VARCHAR(100),
    cap             VARCHAR(10),
    provincia       VARCHAR(50),
    note            TEXT,
    stato           INTEGER DEFAULT 1,
    consenso        BOOLEAN DEFAULT FALSE,
    blacklist       BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX idx_contatti_telefono  ON contatti(telefono);
CREATE INDEX idx_contatti_email     ON contatti(email);
CREATE INDEX idx_contatti_stato     ON contatti(stato);
CREATE INDEX idx_contatti_blacklist ON contatti(blacklist);
CREATE INDEX idx_contatti_cognome   ON contatti(cognome);

-- ============================================================================
-- LISTE (raggruppamento contatti / campagne)
-- ============================================================================

CREATE TABLE liste (
    id          SERIAL PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL UNIQUE,
    descrizione TEXT,
    consenso    BOOLEAN DEFAULT FALSE,
    stato       INTEGER DEFAULT 1,
    scadenza    DATE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX idx_liste_stato      ON liste(stato);
CREATE INDEX idx_liste_scadenza   ON liste(scadenza);
CREATE INDEX idx_liste_deleted_at ON liste(deleted_at);

-- ============================================================================
-- RELAZIONE LISTE-CONTATTI (Many-to-Many)
-- ============================================================================

CREATE TABLE lista_contatti (
    id          SERIAL PRIMARY KEY,
    lista_id    INTEGER NOT NULL REFERENCES liste(id)    ON DELETE CASCADE,
    contatto_id INTEGER NOT NULL REFERENCES contatti(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(lista_id, contatto_id)
);

CREATE INDEX idx_lista_contatti_lista    ON lista_contatti(lista_id);
CREATE INDEX idx_lista_contatti_contatto ON lista_contatti(contatto_id);

-- ============================================================================
-- SESSIONI DI IMPORTAZIONE
-- ============================================================================

CREATE TABLE import_sessions (
    id             VARCHAR(36)  PRIMARY KEY,
    filename       VARCHAR(255) NOT NULL,
    file_path      VARCHAR(500),
    row_count      INTEGER      NOT NULL DEFAULT 0,
    headers        TEXT,
    preview        TEXT,
    column_mapping TEXT,
    status         VARCHAR(50)  NOT NULL DEFAULT 'uploaded',
    error_message  TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP,
    completed_at   TIMESTAMP
);

CREATE INDEX idx_import_sessions_status  ON import_sessions(status);
CREATE INDEX idx_import_sessions_created ON import_sessions(created_at);

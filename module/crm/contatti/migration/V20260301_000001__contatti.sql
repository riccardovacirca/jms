-- ============================================================================
-- MODULO CONTATTI
-- ============================================================================

CREATE TABLE jms_contatti (
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

CREATE INDEX idx_jms_contatti_telefono  ON jms_contatti(telefono);
CREATE INDEX idx_jms_contatti_email     ON jms_contatti(email);
CREATE INDEX idx_jms_contatti_stato     ON jms_contatti(stato);
CREATE INDEX idx_jms_contatti_blacklist ON jms_contatti(blacklist);
CREATE INDEX idx_jms_contatti_cognome   ON jms_contatti(cognome);

-- ============================================================================
-- LISTE (raggruppamento contatti / campagne)
-- ============================================================================

CREATE TABLE jms_liste (
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

CREATE INDEX idx_jms_liste_stato      ON jms_liste(stato);
CREATE INDEX idx_jms_liste_scadenza   ON jms_liste(scadenza);
CREATE INDEX idx_jms_liste_deleted_at ON jms_liste(deleted_at);

-- ============================================================================
-- RELAZIONE LISTE-CONTATTI (Many-to-Many)
-- ============================================================================

CREATE TABLE jms_lista_contatti (
    id          SERIAL PRIMARY KEY,
    lista_id    INTEGER NOT NULL REFERENCES jms_liste(id)    ON DELETE CASCADE,
    contatto_id INTEGER NOT NULL REFERENCES jms_contatti(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(lista_id, contatto_id)
);

CREATE INDEX idx_jms_lista_contatti_lista    ON jms_lista_contatti(lista_id);
CREATE INDEX idx_jms_lista_contatti_contatto ON jms_lista_contatti(contatto_id);

-- ============================================================================
-- SESSIONI DI IMPORTAZIONE
-- ============================================================================

CREATE TABLE jms_import_sessions (
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

CREATE INDEX idx_jms_import_sessions_status  ON jms_import_sessions(status);
CREATE INDEX idx_jms_import_sessions_created ON jms_import_sessions(created_at);

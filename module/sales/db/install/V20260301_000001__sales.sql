-- ============================================================================
-- CONTATTI
-- ============================================================================

CREATE TABLE jms_sales_contatti (
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
    stato           INTEGER   DEFAULT 1,
    consenso        BOOLEAN   DEFAULT FALSE,
    blacklist       BOOLEAN   DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX jms_sales_idx_contatti_telefono  ON jms_sales_contatti(telefono);
CREATE INDEX jms_sales_idx_contatti_email     ON jms_sales_contatti(email);
CREATE INDEX jms_sales_idx_contatti_stato     ON jms_sales_contatti(stato);
CREATE INDEX jms_sales_idx_contatti_blacklist ON jms_sales_contatti(blacklist);
CREATE INDEX jms_sales_idx_contatti_cognome   ON jms_sales_contatti(cognome);

-- ============================================================================
-- LISTE (raggruppamento contatti / campagne)
-- ============================================================================

CREATE TABLE jms_sales_liste (
    id          SERIAL PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL UNIQUE,
    descrizione TEXT,
    consenso    BOOLEAN   DEFAULT FALSE,
    stato       INTEGER   DEFAULT 1,
    scadenza    DATE,
    is_default  BOOLEAN   NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX        jms_sales_idx_liste_stato      ON jms_sales_liste(stato);
CREATE INDEX        jms_sales_idx_liste_scadenza   ON jms_sales_liste(scadenza);
CREATE INDEX        jms_sales_idx_liste_deleted_at ON jms_sales_liste(deleted_at);
CREATE UNIQUE INDEX jms_sales_idx_liste_is_default ON jms_sales_liste(is_default) WHERE is_default = TRUE;

-- ============================================================================
-- RELAZIONE LISTE-CONTATTI (Many-to-Many)
-- ============================================================================

CREATE TABLE jms_sales_lista_contatti (
    id          SERIAL PRIMARY KEY,
    lista_id    INTEGER   NOT NULL REFERENCES jms_sales_liste(id)    ON DELETE CASCADE,
    contatto_id INTEGER   NOT NULL REFERENCES jms_sales_contatti(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(lista_id, contatto_id)
);

CREATE INDEX jms_sales_idx_lista_contatti_lista    ON jms_sales_lista_contatti(lista_id);
CREATE INDEX jms_sales_idx_lista_contatti_contatto ON jms_sales_lista_contatti(contatto_id);

-- ============================================================================
-- SESSIONI DI IMPORTAZIONE
-- ============================================================================

CREATE TABLE jms_sales_import_sessions (
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

CREATE INDEX jms_sales_idx_import_sessions_status  ON jms_sales_import_sessions(status);
CREATE INDEX jms_sales_idx_import_sessions_created ON jms_sales_import_sessions(created_at);

-- ============================================================================
-- CAMPAGNE
-- ============================================================================

CREATE TABLE jms_sales_campagne (
    id          SERIAL PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    descrizione TEXT,
    stato       INTEGER   NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE UNIQUE INDEX jms_sales_idx_campagne_nome ON jms_sales_campagne(nome) WHERE deleted_at IS NULL;
CREATE INDEX        jms_sales_idx_campagne_stato ON jms_sales_campagne(stato);

-- ============================================================================
-- RELAZIONE CAMPAGNE-LISTE (Many-to-Many)
-- ============================================================================

CREATE TABLE jms_sales_campagna_liste (
    campagna_id INTEGER   NOT NULL REFERENCES jms_sales_campagne(id) ON DELETE CASCADE,
    lista_id    INTEGER   NOT NULL REFERENCES jms_sales_liste(id)    ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (campagna_id, lista_id)
);

CREATE INDEX jms_sales_idx_campagna_liste_campagna ON jms_sales_campagna_liste(campagna_id);
CREATE INDEX jms_sales_idx_campagna_liste_lista    ON jms_sales_campagna_liste(lista_id);

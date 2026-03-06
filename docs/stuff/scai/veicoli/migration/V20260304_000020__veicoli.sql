-- ============================================================================
-- MODULO VEICOLI - Database Migration
-- ============================================================================
-- Creazione tabelle per la gestione dell'anagrafica veicoli e convenzioni
--
-- Dipendenze:
--   - scai_ente (modulo ente)
--   - scai_rapporto (modulo rapporti)
--
-- Ordine di installazione: 20
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Tabella: scai_veicolo_tipo
-- Descrizione: Lookup table per tipologie di veicoli (Auto, Moto, Furgone, etc.)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_veicolo_tipo (
    id                      BIGSERIAL PRIMARY KEY,
    codice_tipo             VARCHAR(10) NOT NULL,
    descrizione             VARCHAR(128) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by              BIGINT,
    is_not_deleted          SMALLINT DEFAULT 1,

    CONSTRAINT uq_veicolo_tipo_codice
        UNIQUE (codice_tipo, is_not_deleted)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_veicolo_tipo_codice
    ON scai_veicolo_tipo(codice_tipo);
CREATE INDEX IF NOT EXISTS idx_veicolo_tipo_is_not_deleted
    ON scai_veicolo_tipo(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_veicolo_tipo IS 'Tipologie di veicoli (lookup)';
COMMENT ON COLUMN scai_veicolo_tipo.codice_tipo IS 'Codice univoco tipologia';
COMMENT ON COLUMN scai_veicolo_tipo.descrizione IS 'Descrizione tipologia veicolo';

-- ----------------------------------------------------------------------------
-- Tabella: scai_veicolo_alimentazione
-- Descrizione: Lookup table per tipi di alimentazione (Benzina, Diesel, Elettrico, etc.)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_veicolo_alimentazione (
    id                      BIGSERIAL PRIMARY KEY,
    codice_alimentazione    VARCHAR(10) NOT NULL,
    descrizione             VARCHAR(128) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by              BIGINT,
    is_not_deleted          SMALLINT DEFAULT 1,

    CONSTRAINT uq_veicolo_alimentazione_codice
        UNIQUE (codice_alimentazione, is_not_deleted)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_veicolo_alimentazione_codice
    ON scai_veicolo_alimentazione(codice_alimentazione);
CREATE INDEX IF NOT EXISTS idx_veicolo_alimentazione_is_not_deleted
    ON scai_veicolo_alimentazione(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_veicolo_alimentazione IS 'Tipi di alimentazione veicoli (lookup)';
COMMENT ON COLUMN scai_veicolo_alimentazione.codice_alimentazione IS 'Codice univoco alimentazione';
COMMENT ON COLUMN scai_veicolo_alimentazione.descrizione IS 'Descrizione tipo alimentazione';

-- ----------------------------------------------------------------------------
-- Tabella: scai_veicolo
-- Descrizione: Anagrafica veicoli con targa, modello, tipo e alimentazione
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_veicolo (
    id                      BIGSERIAL PRIMARY KEY,
    targa                   VARCHAR(20) NOT NULL,
    modello                 VARCHAR(128),
    marca                   VARCHAR(128),
    colore                  VARCHAR(64),
    anno_immatricolazione   INTEGER,
    veicolo_tipo_id         BIGINT,
    veicolo_alimentazione_id BIGINT,
    note                    TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by              BIGINT,
    is_not_deleted          SMALLINT DEFAULT 1,

    CONSTRAINT uq_veicolo_targa
        UNIQUE (targa, is_not_deleted)

    -- Foreign keys (opzionali - commentate per gestione a livello applicativo)
    -- CONSTRAINT fk_veicolo_tipo
    --     FOREIGN KEY (veicolo_tipo_id)
    --     REFERENCES scai_veicolo_tipo(id),
    -- CONSTRAINT fk_veicolo_alimentazione
    --     FOREIGN KEY (veicolo_alimentazione_id)
    --     REFERENCES scai_veicolo_alimentazione(id)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_veicolo_targa
    ON scai_veicolo(targa);
CREATE INDEX IF NOT EXISTS idx_veicolo_tipo_id
    ON scai_veicolo(veicolo_tipo_id);
CREATE INDEX IF NOT EXISTS idx_veicolo_alimentazione_id
    ON scai_veicolo(veicolo_alimentazione_id);
CREATE INDEX IF NOT EXISTS idx_veicolo_is_not_deleted
    ON scai_veicolo(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_veicolo IS 'Anagrafica veicoli';
COMMENT ON COLUMN scai_veicolo.targa IS 'Targa veicolo (natural key)';
COMMENT ON COLUMN scai_veicolo.veicolo_tipo_id IS 'FK a scai_veicolo_tipo';
COMMENT ON COLUMN scai_veicolo.veicolo_alimentazione_id IS 'FK a scai_veicolo_alimentazione';

-- ----------------------------------------------------------------------------
-- Tabella: scai_veicolo_convenzione
-- Descrizione: Associazione tra veicoli e dipendenti (rapporti) con stato autorizzazione
-- Dipendenze: scai_veicolo, scai_rapporto (modulo rapporti)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_veicolo_convenzione (
    id                      BIGSERIAL PRIMARY KEY,
    cod_ente                VARCHAR(15) NOT NULL,
    matricola               VARCHAR(16) NOT NULL,
    targa                   VARCHAR(20) NOT NULL,
    data_inizio             DATE,
    data_fine               DATE,
    status                  VARCHAR(20) DEFAULT 'INSERITO',
    note                    TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by              BIGINT,
    is_not_deleted          SMALLINT DEFAULT 1,

    CONSTRAINT uq_veicolo_convenzione
        UNIQUE (cod_ente, matricola, targa, is_not_deleted),

    CONSTRAINT chk_veicolo_convenzione_status
        CHECK (status IN ('INSERITO', 'AUTORIZZATO'))

    -- Foreign keys (opzionali - commentate per gestione a livello applicativo)
    -- CONSTRAINT fk_veicolo_convenzione_rapporto
    --     FOREIGN KEY (cod_ente, matricola)
    --     REFERENCES scai_rapporto(cod_ente, matricola),
    -- CONSTRAINT fk_veicolo_convenzione_veicolo
    --     FOREIGN KEY (targa)
    --     REFERENCES scai_veicolo(targa)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_veicolo_convenzione_cod_ente
    ON scai_veicolo_convenzione(cod_ente);
CREATE INDEX IF NOT EXISTS idx_veicolo_convenzione_matricola
    ON scai_veicolo_convenzione(matricola);
CREATE INDEX IF NOT EXISTS idx_veicolo_convenzione_targa
    ON scai_veicolo_convenzione(targa);
CREATE INDEX IF NOT EXISTS idx_veicolo_convenzione_status
    ON scai_veicolo_convenzione(status);
CREATE INDEX IF NOT EXISTS idx_veicolo_convenzione_is_not_deleted
    ON scai_veicolo_convenzione(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_veicolo_convenzione IS 'Associazione veicoli-dipendenti con stato autorizzazione';
COMMENT ON COLUMN scai_veicolo_convenzione.cod_ente IS 'Codice ente (FK a scai_rapporto)';
COMMENT ON COLUMN scai_veicolo_convenzione.matricola IS 'Matricola dipendente (FK a scai_rapporto)';
COMMENT ON COLUMN scai_veicolo_convenzione.targa IS 'Targa veicolo (FK a scai_veicolo)';
COMMENT ON COLUMN scai_veicolo_convenzione.status IS 'Stato: INSERITO, AUTORIZZATO';

-- ============================================================================
-- Fine Migration
-- ============================================================================

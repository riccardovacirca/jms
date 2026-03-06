-- ============================================================================
-- MODULO POLICY - Database Migration
-- ============================================================================
-- Creazione tabelle per la gestione delle policy di accesso associate ai rapporti
--
-- Dipendenze:
--   - scai_ente (modulo ente)
--   - scai_sede (modulo sede)
--   - scai_rapporto (modulo rapporti)
--   - scai_sistemi_campo (modulo sdc)
--   - scai_repertorio (modulo repertorio)
--
-- Ordine di installazione: 25
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Tabella: scai_policy_rapporto
-- Descrizione: Policy di accesso associate ai rapporti dipendenti
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_policy_rapporto (
    id                      BIGSERIAL PRIMARY KEY,
    cod_ente                VARCHAR(15) NOT NULL,
    matricola               VARCHAR(15) NOT NULL,
    slug_sdc                VARCHAR(64),
    codice_repertorio       VARCHAR(6) NOT NULL,
    codice_policy           VARCHAR(16) NOT NULL,
    data_inizio_validita    TIMESTAMP NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by              VARCHAR(255),
    is_not_deleted          SMALLINT DEFAULT 1,

    CONSTRAINT uq_policy_rapporto
        UNIQUE (cod_ente, matricola, codice_repertorio, codice_policy, is_not_deleted)

    -- Foreign keys (opzionali - commentate per gestione a livello applicativo)
    -- CONSTRAINT fk_policy_rapporto_rapporto
    --     FOREIGN KEY (cod_ente, matricola)
    --     REFERENCES scai_rapporto(cod_ente, matricola),
    -- CONSTRAINT fk_policy_rapporto_sdc
    --     FOREIGN KEY (slug_sdc)
    --     REFERENCES scai_sistemi_campo(slug),
    -- CONSTRAINT fk_policy_rapporto_repertorio
    --     FOREIGN KEY (codice_repertorio)
    --     REFERENCES scai_repertorio(codice_repertorio)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_policy_rapporto_cod_ente
    ON scai_policy_rapporto(cod_ente);
CREATE INDEX IF NOT EXISTS idx_policy_rapporto_matricola
    ON scai_policy_rapporto(matricola);
CREATE INDEX IF NOT EXISTS idx_policy_rapporto_slug_sdc
    ON scai_policy_rapporto(slug_sdc);
CREATE INDEX IF NOT EXISTS idx_policy_rapporto_codice_repertorio
    ON scai_policy_rapporto(codice_repertorio);
CREATE INDEX IF NOT EXISTS idx_policy_rapporto_codice_policy
    ON scai_policy_rapporto(codice_policy);
CREATE INDEX IF NOT EXISTS idx_policy_rapporto_is_not_deleted
    ON scai_policy_rapporto(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_policy_rapporto IS 'Policy di accesso associate ai rapporti dipendenti';
COMMENT ON COLUMN scai_policy_rapporto.cod_ente IS 'Codice ente (FK cross-module a scai_rapporto)';
COMMENT ON COLUMN scai_policy_rapporto.matricola IS 'Matricola dipendente (FK cross-module a scai_rapporto)';
COMMENT ON COLUMN scai_policy_rapporto.slug_sdc IS 'Slug sistema di campo (FK a scai_sistemi_campo)';
COMMENT ON COLUMN scai_policy_rapporto.codice_repertorio IS 'Codice repertorio (FK a scai_repertorio)';
COMMENT ON COLUMN scai_policy_rapporto.codice_policy IS 'Codice policy di accesso';
COMMENT ON COLUMN scai_policy_rapporto.data_inizio_validita IS 'Data inizio validità della policy';

-- ----------------------------------------------------------------------------
-- Tabella: scai_policy_default_sede
-- Descrizione: Policy di default per sede
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_policy_default_sede (
    id                      BIGSERIAL PRIMARY KEY,
    codice_policy           VARCHAR(16) NOT NULL,
    codice_repertorio       VARCHAR(6) NOT NULL,
    cod_ente                VARCHAR(15) NOT NULL,
    cod_sede                VARCHAR(10) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    is_not_deleted          SMALLINT DEFAULT 1,

    CONSTRAINT uq_policy_default_sede
        UNIQUE (codice_policy, codice_repertorio, cod_ente, cod_sede, is_not_deleted)

    -- Foreign keys (opzionali - commentate per gestione a livello applicativo)
    -- CONSTRAINT fk_policy_default_ente
    --     FOREIGN KEY (cod_ente)
    --     REFERENCES scai_ente(cod_ente),
    -- CONSTRAINT fk_policy_default_sede
    --     FOREIGN KEY (cod_ente, cod_sede)
    --     REFERENCES scai_sede(cod_ente, cod_sede),
    -- CONSTRAINT fk_policy_default_repertorio
    --     FOREIGN KEY (codice_repertorio)
    --     REFERENCES scai_repertorio(codice_repertorio)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_policy_default_sede_cod_ente
    ON scai_policy_default_sede(cod_ente);
CREATE INDEX IF NOT EXISTS idx_policy_default_sede_cod_sede
    ON scai_policy_default_sede(cod_sede);
CREATE INDEX IF NOT EXISTS idx_policy_default_sede_codice_repertorio
    ON scai_policy_default_sede(codice_repertorio);
CREATE INDEX IF NOT EXISTS idx_policy_default_sede_is_not_deleted
    ON scai_policy_default_sede(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_policy_default_sede IS 'Policy di default associate alle sedi';
COMMENT ON COLUMN scai_policy_default_sede.codice_policy IS 'Codice policy di default';
COMMENT ON COLUMN scai_policy_default_sede.codice_repertorio IS 'Codice repertorio (FK a scai_repertorio)';
COMMENT ON COLUMN scai_policy_default_sede.cod_ente IS 'Codice ente (FK a scai_ente)';
COMMENT ON COLUMN scai_policy_default_sede.cod_sede IS 'Codice sede (FK a scai_sede)';

-- ----------------------------------------------------------------------------
-- Tabella: scai_policy_rapporto_validity
-- Descrizione: Validità temporale delle policy associate ai rapporti
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_policy_rapporto_validity (
    id                      BIGSERIAL PRIMARY KEY,
    cod_ente                VARCHAR(15) NOT NULL,
    matricola               VARCHAR(15) NOT NULL,
    slug_sdc                VARCHAR(16),
    data_inizio             TIMESTAMP,
    data_fine               TIMESTAMP,
    data_cessazione         TIMESTAMP,
    attivo                  SMALLINT NOT NULL DEFAULT 1,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    is_not_deleted          SMALLINT DEFAULT 1,

    CONSTRAINT uq_policy_rapporto_validity
        UNIQUE (cod_ente, matricola, slug_sdc, is_not_deleted)

    -- Foreign keys (opzionali - commentate per gestione a livello applicativo)
    -- CONSTRAINT fk_policy_validity_rapporto
    --     FOREIGN KEY (cod_ente, matricola)
    --     REFERENCES scai_rapporto(cod_ente, matricola),
    -- CONSTRAINT fk_policy_validity_sdc
    --     FOREIGN KEY (slug_sdc)
    --     REFERENCES scai_sistemi_campo(slug)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_policy_validity_cod_ente
    ON scai_policy_rapporto_validity(cod_ente);
CREATE INDEX IF NOT EXISTS idx_policy_validity_matricola
    ON scai_policy_rapporto_validity(matricola);
CREATE INDEX IF NOT EXISTS idx_policy_validity_slug_sdc
    ON scai_policy_rapporto_validity(slug_sdc);
CREATE INDEX IF NOT EXISTS idx_policy_validity_attivo
    ON scai_policy_rapporto_validity(attivo);
CREATE INDEX IF NOT EXISTS idx_policy_validity_is_not_deleted
    ON scai_policy_rapporto_validity(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_policy_rapporto_validity IS 'Validità temporale delle policy associate ai rapporti';
COMMENT ON COLUMN scai_policy_rapporto_validity.cod_ente IS 'Codice ente (FK cross-module a scai_rapporto)';
COMMENT ON COLUMN scai_policy_rapporto_validity.matricola IS 'Matricola dipendente (FK cross-module a scai_rapporto)';
COMMENT ON COLUMN scai_policy_rapporto_validity.slug_sdc IS 'Slug sistema di campo (FK a scai_sistemi_campo)';
COMMENT ON COLUMN scai_policy_rapporto_validity.data_inizio IS 'Data inizio validità';
COMMENT ON COLUMN scai_policy_rapporto_validity.data_fine IS 'Data fine validità';
COMMENT ON COLUMN scai_policy_rapporto_validity.data_cessazione IS 'Data cessazione validità';
COMMENT ON COLUMN scai_policy_rapporto_validity.attivo IS 'Flag attivo (1=attivo, 0=non attivo)';

-- ============================================================================
-- Fine Migration
-- ============================================================================

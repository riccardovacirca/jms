-- ============================================================================
-- MODULO BADGE - Database Migration
-- ============================================================================
-- Creazione tabelle per la gestione dell'anagrafica badge e richieste badge
--
-- Dipendenze:
--   - scai_ente (modulo ente)
--   - scai_rapporto (modulo rapporti)
--
-- Ordine di installazione: 30
--
-- ⚠️ ATTENZIONE - INCONSISTENZA RILEVATA NEL DATABASE ESISTENTE:
-- - scai_badge usa id_ente (BIGINT, FK a scai_ente.id)
-- - scai_badge_request usa cod_ente (VARCHAR, natural key)
-- Questa inconsistenza potrebbe essere un BUG nel design originale del database.
-- La migration replica fedelmente la struttura esistente ma il team dovrebbe
-- valutare se standardizzare l'approccio (preferibilmente usando cod_ente VARCHAR
-- per coerenza con altri moduli come policy, veicoli, etc.)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Tabella: scai_badge_tipo
-- Descrizione: Lookup table per tipologie di badge
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_badge_tipo (
    id                          BIGSERIAL PRIMARY KEY,
    cod_tipo_badge              VARCHAR(2) NOT NULL,
    descrizione_tipo_badge      VARCHAR(255) NOT NULL,
    is_not_deleted              SMALLINT DEFAULT 1,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_badge_tipo_cod
        UNIQUE (cod_tipo_badge, is_not_deleted)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_badge_tipo_cod
    ON scai_badge_tipo(cod_tipo_badge);
CREATE INDEX IF NOT EXISTS idx_badge_tipo_is_not_deleted
    ON scai_badge_tipo(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_badge_tipo IS 'Tipologie di badge (lookup)';
COMMENT ON COLUMN scai_badge_tipo.cod_tipo_badge IS 'Codice univoco tipologia badge';
COMMENT ON COLUMN scai_badge_tipo.descrizione_tipo_badge IS 'Descrizione tipologia';

-- ----------------------------------------------------------------------------
-- Tabella: scai_badge_causale_emissione
-- Descrizione: Lookup table per causali di emissione badge
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_badge_causale_emissione (
    id                          BIGSERIAL PRIMARY KEY,
    cod_causale_emissione       VARCHAR(8) NOT NULL,
    desc_causale_emissione      VARCHAR(255) NOT NULL,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_badge_causale_cod
        UNIQUE (cod_causale_emissione)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_badge_causale_cod
    ON scai_badge_causale_emissione(cod_causale_emissione);

-- Commenti
COMMENT ON TABLE scai_badge_causale_emissione IS 'Causali di emissione badge (lookup)';
COMMENT ON COLUMN scai_badge_causale_emissione.cod_causale_emissione IS 'Codice univoco causale';
COMMENT ON COLUMN scai_badge_causale_emissione.desc_causale_emissione IS 'Descrizione causale emissione';

-- ----------------------------------------------------------------------------
-- Tabella: scai_badge
-- Descrizione: Anagrafica principale badge
-- ⚠️ NOTE: Usa id_ente (BIGINT) invece di cod_ente (VARCHAR)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_badge (
    id                          BIGSERIAL PRIMARY KEY,
    seq_id                      INTEGER NOT NULL,
    id_ente                     BIGINT NOT NULL,        -- ⚠️ BIGINT invece di VARCHAR!
    id_tipo_tessera             BIGINT NOT NULL,
    numero                      VARCHAR(20) NOT NULL,
    matricola                   VARCHAR(15) NOT NULL,
    cod_fis                     VARCHAR(16) NOT NULL,
    cognome                     VARCHAR(128) NOT NULL,
    nome                        VARCHAR(128) NOT NULL,
    tecnologia                  VARCHAR(8) NOT NULL,
    data_inizio_validita        TIMESTAMP NOT NULL DEFAULT NOW(),
    data_fine_validita          TIMESTAMP NOT NULL DEFAULT NOW(),
    data_produzione             TIMESTAMP NOT NULL DEFAULT NOW(),
    data_ritiro                 TIMESTAMP NOT NULL DEFAULT NOW(),
    attivo                      SMALLINT NOT NULL DEFAULT 1,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    is_not_deleted              SMALLINT DEFAULT 1,

    CONSTRAINT uq_badge_matricola_ente
        UNIQUE (matricola, id_ente, is_not_deleted)

    -- Foreign keys (opzionali - commentate per gestione a livello applicativo)
    -- ⚠️ NOTE: Queste FK usano id_ente (BIGINT) invece del pattern standard cod_ente
    -- CONSTRAINT fk_badge_ente
    --     FOREIGN KEY (id_ente)
    --     REFERENCES scai_ente(id),
    -- CONSTRAINT fk_badge_tipo
    --     FOREIGN KEY (id_tipo_tessera)
    --     REFERENCES scai_badge_tipo(id)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_badge_id_ente
    ON scai_badge(id_ente);
CREATE INDEX IF NOT EXISTS idx_badge_matricola
    ON scai_badge(matricola);
CREATE INDEX IF NOT EXISTS idx_badge_numero
    ON scai_badge(numero);
CREATE INDEX IF NOT EXISTS idx_badge_cod_fis
    ON scai_badge(cod_fis);
CREATE INDEX IF NOT EXISTS idx_badge_id_tipo_tessera
    ON scai_badge(id_tipo_tessera);
CREATE INDEX IF NOT EXISTS idx_badge_attivo
    ON scai_badge(attivo);
CREATE INDEX IF NOT EXISTS idx_badge_is_not_deleted
    ON scai_badge(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_badge IS 'Anagrafica badge dipendenti';
COMMENT ON COLUMN scai_badge.id_ente IS '⚠️ ATTENZIONE: BIGINT FK a scai_ente.id (inconsistenza con altri moduli)';
COMMENT ON COLUMN scai_badge.id_tipo_tessera IS 'FK a scai_badge_tipo.id';
COMMENT ON COLUMN scai_badge.numero IS 'Numero badge';
COMMENT ON COLUMN scai_badge.matricola IS 'Matricola dipendente';
COMMENT ON COLUMN scai_badge.tecnologia IS 'Tecnologia badge (es: RFID, NFC, etc.)';
COMMENT ON COLUMN scai_badge.attivo IS 'Stato attivazione (1=attivo, 0=disattivo)';

-- ----------------------------------------------------------------------------
-- Tabella: scai_badge_request
-- Descrizione: Richieste di assegnazione badge
-- ⚠️ NOTE: Usa cod_ente (VARCHAR) - pattern standard ma inconsistente con scai_badge!
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scai_badge_request (
    id                          BIGSERIAL PRIMARY KEY,
    cod_ente                    VARCHAR(10) NOT NULL,   -- ⚠️ VARCHAR (diverso da scai_badge!)
    matricola                   VARCHAR(20) NOT NULL,
    cod_fis                     VARCHAR(16) NOT NULL,
    data_inizio_validita        TIMESTAMP NOT NULL DEFAULT NOW(),
    instance_id                 BIGINT NOT NULL,
    is_not_deleted              SMALLINT DEFAULT 1,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_badge_request_cod_ente_matricola
        UNIQUE (cod_ente, matricola, is_not_deleted)

    -- Foreign keys (opzionali - commentate per gestione a livello applicativo)
    -- ⚠️ NOTE: Impossibile creare FK diretta con scai_badge a causa di inconsistenza chiavi
    -- CONSTRAINT fk_badge_request_rapporto
    --     FOREIGN KEY (cod_ente, matricola)
    --     REFERENCES scai_rapporto(cod_ente, matricola)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_badge_request_cod_ente
    ON scai_badge_request(cod_ente);
CREATE INDEX IF NOT EXISTS idx_badge_request_matricola
    ON scai_badge_request(matricola);
CREATE INDEX IF NOT EXISTS idx_badge_request_cod_fis
    ON scai_badge_request(cod_fis);
CREATE INDEX IF NOT EXISTS idx_badge_request_instance_id
    ON scai_badge_request(instance_id);
CREATE INDEX IF NOT EXISTS idx_badge_request_is_not_deleted
    ON scai_badge_request(is_not_deleted);

-- Commenti
COMMENT ON TABLE scai_badge_request IS 'Richieste di assegnazione badge ai dipendenti';
COMMENT ON COLUMN scai_badge_request.cod_ente IS '⚠️ ATTENZIONE: VARCHAR natural key (inconsistenza con scai_badge.id_ente)';
COMMENT ON COLUMN scai_badge_request.matricola IS 'Matricola dipendente';
COMMENT ON COLUMN scai_badge_request.instance_id IS 'ID istanza workflow (integrazione sistema esterno)';

-- ============================================================================
-- NOTE FINALI
-- ============================================================================
-- RISOLUZIONE CONSIGLIATA DELL'INCONSISTENZA:
--
-- Opzione 1 (PREFERITA - Standard con altri moduli):
--   - Modificare scai_badge per usare cod_ente VARCHAR invece di id_ente BIGINT
--   - Pro: Coerenza con policy, veicoli, rapporti, e pattern generale
--   - Contro: Richiede migration dei dati esistenti
--
-- Opzione 2 (Alternativa):
--   - Modificare scai_badge_request per usare id_ente BIGINT
--   - Pro: Minimizza cambiamenti su tabella principale
--   - Contro: Perde coerenza con pattern cross-module standard
--
-- Opzione 3 (Temporanea):
--   - Mantenere entrambe le chiavi con join via scai_ente
--   - Pro: Nessuna migration immediata
--   - Contro: Complessità query, prestazioni ridotte
-- ============================================================================

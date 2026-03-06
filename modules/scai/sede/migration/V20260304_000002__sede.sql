-- ============================================================================
-- MODULO SEDE - Sistema SCAI
-- ============================================================================
-- NOTA: Questo modulo gestisce l'anagrafica delle sedi di lavoro.
-- È un modulo di tipo LOOKUP utilizzato da altri moduli (rapporti, badge, ecc.)
-- ============================================================================

-- ============================================================================
-- TABELLA: scai_sede
-- ============================================================================
-- Anagrafica sedi di lavoro

CREATE TABLE IF NOT EXISTS scai_sede (
    id                  BIGSERIAL PRIMARY KEY,
    cod_sede            VARCHAR(10) NOT NULL,
    nome                VARCHAR(255) NOT NULL,
    indirizzo           VARCHAR(512) NOT NULL,
    cap                 VARCHAR(5),
    city_code           VARCHAR(4),
    province_code       VARCHAR(4),
    istat_region        VARCHAR(2),
    nazione             VARCHAR(2),
    descrizione_breve   VARCHAR(255) NOT NULL,
    descrizione_lunga   VARCHAR(512) NOT NULL,

    -- Audit fields
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    is_not_deleted      SMALLINT DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_sede_cod_sede
        UNIQUE (cod_sede, is_not_deleted)
);

-- Indici per performance
CREATE INDEX IF NOT EXISTS idx_sede_cod_sede ON scai_sede(cod_sede);
CREATE INDEX IF NOT EXISTS idx_sede_nome ON scai_sede(nome);
CREATE INDEX IF NOT EXISTS idx_sede_city_code ON scai_sede(city_code);
CREATE INDEX IF NOT EXISTS idx_sede_province_code ON scai_sede(province_code);
CREATE INDEX IF NOT EXISTS idx_sede_is_not_deleted ON scai_sede(is_not_deleted);

COMMENT ON TABLE scai_sede IS 'Anagrafica sedi di lavoro - Sistema SCAI';
COMMENT ON COLUMN scai_sede.cod_sede IS 'Codice sede (max 10 caratteri)';
COMMENT ON COLUMN scai_sede.nome IS 'Nome sede';
COMMENT ON COLUMN scai_sede.indirizzo IS 'Indirizzo completo sede';
COMMENT ON COLUMN scai_sede.cap IS 'Codice Avviamento Postale';
COMMENT ON COLUMN scai_sede.city_code IS 'Codice comune ISTAT';
COMMENT ON COLUMN scai_sede.province_code IS 'Codice provincia';
COMMENT ON COLUMN scai_sede.istat_region IS 'Codice ISTAT regione';
COMMENT ON COLUMN scai_sede.nazione IS 'Codice nazione ISO';
COMMENT ON COLUMN scai_sede.descrizione_breve IS 'Descrizione breve sede';
COMMENT ON COLUMN scai_sede.descrizione_lunga IS 'Descrizione completa sede';
COMMENT ON COLUMN scai_sede.is_not_deleted IS 'Soft delete: 1=attivo, NULL=cancellato';

-- ============================================================================
-- TRIGGER PER AUTO-UPDATE DI updated_at
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_scai_sede_updated_at ON scai_sede;
CREATE TRIGGER update_scai_sede_updated_at
    BEFORE UPDATE ON scai_sede
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- NOTE SULL'ARCHITETTURA MODULARE
-- ============================================================================
-- Questo modulo è di tipo LOOKUP e fornisce dati di riferimento per altri moduli.
-- È utilizzato da:
--   - modulo rapporti → scai_rapporto(cod_sede_primaria, cod_sede_secondaria) FK scai_sede
--   - modulo badge → scai_badge(cod_sede) FK scai_sede (se implementato)
--   - modulo policy → scai_policy_rapporto(cod_sede) FK scai_sede (se implementato)
--
-- Questo modulo ha installation order = 2 (nessuna dipendenza).

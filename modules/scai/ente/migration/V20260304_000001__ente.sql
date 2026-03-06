-- ============================================================================
-- MODULO ENTE - Sistema SCAI
-- ============================================================================
-- NOTA: Questo modulo gestisce l'anagrafica degli enti regionali.
-- È un modulo di tipo LOOKUP utilizzato da altri moduli (rapporti, badge, ecc.)
-- ============================================================================

-- ============================================================================
-- TABELLA: scai_ente
-- ============================================================================
-- Anagrafica enti regionali

CREATE TABLE IF NOT EXISTS scai_ente (
    id                  BIGSERIAL PRIMARY KEY,
    cod_ente            VARCHAR(15) NOT NULL,
    descrizione_ente    VARCHAR(255) NOT NULL,
    flag_areas          SMALLINT DEFAULT 0,
    id_azienda_areas    BIGINT,

    -- Audit fields
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by          BIGINT,
    is_not_deleted      SMALLINT DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_ente_cod_ente
        UNIQUE (cod_ente, is_not_deleted)
);

-- Indici per performance
CREATE INDEX IF NOT EXISTS idx_ente_cod_ente ON scai_ente(cod_ente);
CREATE INDEX IF NOT EXISTS idx_ente_is_not_deleted ON scai_ente(is_not_deleted);

COMMENT ON TABLE scai_ente IS 'Anagrafica enti regionali - Sistema SCAI';
COMMENT ON COLUMN scai_ente.cod_ente IS 'Codice ente regionale (max 15 caratteri)';
COMMENT ON COLUMN scai_ente.descrizione_ente IS 'Descrizione completa dell''ente';
COMMENT ON COLUMN scai_ente.flag_areas IS 'Flag integrazione con sistema AREAS (0=disabilitato, 1=abilitato)';
COMMENT ON COLUMN scai_ente.id_azienda_areas IS 'ID azienda nel sistema AREAS';
COMMENT ON COLUMN scai_ente.is_not_deleted IS 'Soft delete: 1=attivo, NULL=cancellato';

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

DROP TRIGGER IF EXISTS update_scai_ente_updated_at ON scai_ente;
CREATE TRIGGER update_scai_ente_updated_at
    BEFORE UPDATE ON scai_ente
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- NOTE SULL'ARCHITETTURA MODULARE
-- ============================================================================
-- Questo modulo è di tipo LOOKUP e fornisce dati di riferimento per altri moduli.
-- È utilizzato da:
--   - modulo rapporti → scai_rapporto(cod_ente) FK scai_ente
--   - modulo badge → scai_badge(cod_ente) FK scai_ente
--   - modulo policy → scai_policy_rapporto(cod_ente) FK scai_ente
--   - modulo veicoli → scai_veicolo(cod_ente) FK scai_ente
--
-- Questo modulo ha installation order = 1 (nessuna dipendenza).

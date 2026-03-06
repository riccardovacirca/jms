-- ============================================================================
-- MODULO SDC (Sistemi di Campo) - Sistema SCAI
-- ============================================================================
-- NOTA: Questo modulo gestisce l'anagrafica dei sistemi di campo.
-- È un modulo di tipo LOOKUP utilizzato da altri moduli (policy, ecc.)
-- ============================================================================

-- ============================================================================
-- TABELLA: scai_sistemi_campo
-- ============================================================================
-- Anagrafica sistemi di campo

CREATE TABLE IF NOT EXISTS scai_sistemi_campo (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(8) NOT NULL,
    slug                VARCHAR(64) NOT NULL,
    descrizione_breve   VARCHAR(255) NOT NULL,
    descrizione_lunga   VARCHAR(512) NOT NULL,

    -- Audit fields
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    is_not_deleted      SMALLINT DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_sdc_slug
        UNIQUE (slug, is_not_deleted)
);

-- Indici per performance
CREATE INDEX IF NOT EXISTS idx_sdc_slug ON scai_sistemi_campo(slug);
CREATE INDEX IF NOT EXISTS idx_sdc_code ON scai_sistemi_campo(code);
CREATE INDEX IF NOT EXISTS idx_sdc_is_not_deleted ON scai_sistemi_campo(is_not_deleted);

COMMENT ON TABLE scai_sistemi_campo IS 'Anagrafica sistemi di campo - Sistema SCAI';
COMMENT ON COLUMN scai_sistemi_campo.code IS 'Codice legacy SCAI (max 8 caratteri)';
COMMENT ON COLUMN scai_sistemi_campo.slug IS 'Identificatore univoco slug (max 64 caratteri)';
COMMENT ON COLUMN scai_sistemi_campo.descrizione_breve IS 'Descrizione breve sistema di campo';
COMMENT ON COLUMN scai_sistemi_campo.descrizione_lunga IS 'Descrizione completa sistema di campo';
COMMENT ON COLUMN scai_sistemi_campo.is_not_deleted IS 'Soft delete: 1=attivo, NULL=cancellato';

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

DROP TRIGGER IF EXISTS update_scai_sistemi_campo_updated_at ON scai_sistemi_campo;
CREATE TRIGGER update_scai_sistemi_campo_updated_at
    BEFORE UPDATE ON scai_sistemi_campo
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- NOTE SULL'ARCHITETTURA MODULARE
-- ============================================================================
-- Questo modulo è di tipo LOOKUP e fornisce dati di riferimento per altri moduli.
-- È utilizzato da:
--   - modulo policy → scai_policy_rapporto FK scai_sistemi_campo
--
-- Questo modulo ha installation order = 3 (nessuna dipendenza).

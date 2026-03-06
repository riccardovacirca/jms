-- ============================================================================
-- MODULO REPERTORIO - Sistema SCAI
-- ============================================================================
-- NOTA: Questo modulo gestisce l'anagrafica dei repertori.
-- È un modulo di tipo LOOKUP utilizzato da altri moduli (policy, ecc.)
-- ============================================================================

-- ============================================================================
-- TABELLA: scai_repertorio
-- ============================================================================
-- Anagrafica repertori

CREATE TABLE IF NOT EXISTS scai_repertorio (
    id                  BIGSERIAL PRIMARY KEY,
    codice_repertorio   VARCHAR(6) NOT NULL,
    descrizione         VARCHAR(255),
    slug_sdc            VARCHAR(32) NOT NULL,
    livello             VARCHAR(8),
    flag_parcheggio     VARCHAR(4),
    flag_struttura      VARCHAR(4),

    -- Audit fields
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    is_not_deleted      SMALLINT DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_repertorio_codice
        UNIQUE (codice_repertorio, is_not_deleted)
);

-- Indici per performance
CREATE INDEX IF NOT EXISTS idx_repertorio_codice ON scai_repertorio(codice_repertorio);
CREATE INDEX IF NOT EXISTS idx_repertorio_slug_sdc ON scai_repertorio(slug_sdc);
CREATE INDEX IF NOT EXISTS idx_repertorio_is_not_deleted ON scai_repertorio(is_not_deleted);

COMMENT ON TABLE scai_repertorio IS 'Anagrafica repertori - Sistema SCAI';
COMMENT ON COLUMN scai_repertorio.codice_repertorio IS 'Codice univoco repertorio (max 6 caratteri)';
COMMENT ON COLUMN scai_repertorio.descrizione IS 'Descrizione repertorio';
COMMENT ON COLUMN scai_repertorio.slug_sdc IS 'Riferimento a sistema di campo (slug)';
COMMENT ON COLUMN scai_repertorio.livello IS 'Tipologia/livello repertorio';
COMMENT ON COLUMN scai_repertorio.flag_parcheggio IS 'Flag parcheggio (max 4 caratteri)';
COMMENT ON COLUMN scai_repertorio.flag_struttura IS 'Flag struttura (max 4 caratteri)';
COMMENT ON COLUMN scai_repertorio.is_not_deleted IS 'Soft delete: 1=attivo, NULL=cancellato';

-- ============================================================================
-- FOREIGN KEYS (opzionali)
-- ============================================================================
-- Le FK sono commentate perché il backend Laravel gestisce le relazioni
-- a livello applicativo. Decommentare se si implementa backend Java/Spring Boot.

-- Relazione con sistema di campo
-- ALTER TABLE scai_repertorio
--     ADD CONSTRAINT fk_repertorio_sdc
--     FOREIGN KEY (slug_sdc)
--     REFERENCES scai_sistemi_campo(slug)
--     ON DELETE RESTRICT;

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

DROP TRIGGER IF EXISTS update_scai_repertorio_updated_at ON scai_repertorio;
CREATE TRIGGER update_scai_repertorio_updated_at
    BEFORE UPDATE ON scai_repertorio
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- NOTE SULL'ARCHITETTURA MODULARE
-- ============================================================================
-- Questo modulo è di tipo LOOKUP e fornisce dati di riferimento per altri moduli.
-- È utilizzato da:
--   - modulo policy → scai_policy_rapporto FK scai_repertorio
--
-- Dipende da:
--   - modulo sdc → scai_repertorio(slug_sdc) FK scai_sistemi_campo
--
-- Questo modulo ha installation order = 4 (dipende da sdc).

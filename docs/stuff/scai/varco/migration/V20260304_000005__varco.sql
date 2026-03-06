-- ============================================================================
-- MODULO VARCO (Varchi) - Sistema SCAI
-- ============================================================================
-- NOTA: Questo modulo gestisce l'anagrafica dei varchi di accesso.
-- È un modulo di tipo LOOKUP utilizzato da altri moduli (policy, ecc.)
-- ============================================================================

-- ============================================================================
-- TABELLA: scai_sistemi_campo_varchi
-- ============================================================================
-- Anagrafica varchi di accesso

CREATE TABLE IF NOT EXISTS scai_sistemi_campo_varchi (
    id                  BIGSERIAL PRIMARY KEY,
    codice_varco        VARCHAR(6),
    desc_ridotta        VARCHAR(128),
    desc_lunga          VARCHAR(256),
    cod_repertorio      VARCHAR(6),
    slug_sdc            VARCHAR(64),

    -- Audit fields
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by          BIGINT,
    is_not_deleted      SMALLINT DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_varco_codice_repertorio
        UNIQUE (cod_repertorio, codice_varco, is_not_deleted)
);

-- Indici per performance
CREATE INDEX IF NOT EXISTS idx_varco_codice ON scai_sistemi_campo_varchi(codice_varco);
CREATE INDEX IF NOT EXISTS idx_varco_cod_repertorio ON scai_sistemi_campo_varchi(cod_repertorio);
CREATE INDEX IF NOT EXISTS idx_varco_slug_sdc ON scai_sistemi_campo_varchi(slug_sdc);
CREATE INDEX IF NOT EXISTS idx_varco_is_not_deleted ON scai_sistemi_campo_varchi(is_not_deleted);

COMMENT ON TABLE scai_sistemi_campo_varchi IS 'Anagrafica varchi di accesso - Sistema SCAI';
COMMENT ON COLUMN scai_sistemi_campo_varchi.codice_varco IS 'Codice varco (max 6 caratteri)';
COMMENT ON COLUMN scai_sistemi_campo_varchi.desc_ridotta IS 'Descrizione breve varco (max 128 caratteri)';
COMMENT ON COLUMN scai_sistemi_campo_varchi.desc_lunga IS 'Descrizione completa varco (max 256 caratteri)';
COMMENT ON COLUMN scai_sistemi_campo_varchi.cod_repertorio IS 'Riferimento a repertorio';
COMMENT ON COLUMN scai_sistemi_campo_varchi.slug_sdc IS 'Riferimento a sistema di campo';
COMMENT ON COLUMN scai_sistemi_campo_varchi.is_not_deleted IS 'Soft delete: 1=attivo, NULL=cancellato';

-- ============================================================================
-- FOREIGN KEYS (opzionali)
-- ============================================================================
-- Le FK sono commentate perché il backend Laravel gestisce le relazioni
-- a livello applicativo. Decommentare se si implementa backend Java/Spring Boot.

-- Relazione con repertorio
-- ALTER TABLE scai_sistemi_campo_varchi
--     ADD CONSTRAINT fk_varco_repertorio
--     FOREIGN KEY (cod_repertorio)
--     REFERENCES scai_repertorio(codice_repertorio)
--     ON DELETE RESTRICT;

-- Relazione con sistema di campo
-- ALTER TABLE scai_sistemi_campo_varchi
--     ADD CONSTRAINT fk_varco_sdc
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

DROP TRIGGER IF EXISTS update_scai_sistemi_campo_varchi_updated_at ON scai_sistemi_campo_varchi;
CREATE TRIGGER update_scai_sistemi_campo_varchi_updated_at
    BEFORE UPDATE ON scai_sistemi_campo_varchi
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- NOTE SULL'ARCHITETTURA MODULARE
-- ============================================================================
-- Questo modulo è di tipo LOOKUP e fornisce dati di riferimento per altri moduli.
-- È utilizzato da:
--   - modulo policy → scai_policy_rapporto FK scai_sistemi_campo_varchi
--
-- Dipende da:
--   - modulo sdc → scai_sistemi_campo_varchi(slug_sdc) FK scai_sistemi_campo
--   - modulo repertorio → scai_sistemi_campo_varchi(cod_repertorio) FK scai_repertorio
--
-- Questo modulo ha installation order = 5 (dipende da sdc e repertorio).

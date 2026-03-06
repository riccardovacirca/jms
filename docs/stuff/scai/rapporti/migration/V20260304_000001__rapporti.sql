-- ============================================================================
-- MODULO RAPPORTI - Sistema SCAI
-- ============================================================================
-- NOTA: Questo modulo gestisce SOLO le tabelle relative ai rapporti di lavoro.
-- Le entità correlate (Badge, Policy, Veicoli) sono gestite da moduli separati.
-- ============================================================================

-- ============================================================================
-- TABELLA PRINCIPALE: scai_rapporto
-- ============================================================================
-- Rappresenta i rapporti di lavoro dei dipendenti

CREATE TABLE IF NOT EXISTS scai_rapporto (
    id                          BIGSERIAL PRIMARY KEY,

    -- Dati identificativi
    cod_ente                    VARCHAR(15) NOT NULL,
    matricola                   VARCHAR(15) NOT NULL,
    cod_rapporto                VARCHAR(15),

    -- Dati anagrafici
    cognome                     VARCHAR(128) NOT NULL,
    nome                        VARCHAR(128) NOT NULL,
    sesso                       VARCHAR(4) NOT NULL,
    cod_fis                     VARCHAR(16) NOT NULL,

    -- Dati contrattuali
    data_assunzione             TIMESTAMP,
    data_cessazione             TIMESTAMP,
    cod_ente_hr                 VARCHAR(16),

    -- Azienda e mansione
    codice_azienda              VARCHAR(15),
    descrizione_azienda         VARCHAR(255),
    codice_mansione             VARCHAR(16),
    descrizione_mansione        VARCHAR(512),

    -- Sedi di lavoro
    cod_sede_primaria           VARCHAR(15),
    descrizione_sede_primaria   VARCHAR(512),
    data_inizio_sede_primaria   TIMESTAMP,
    cod_sede_secondaria         VARCHAR(15),
    descrizione_sede_secondaria VARCHAR(512),
    data_inizio_sede_secondaria TIMESTAMP,

    -- Struttura organizzativa
    codice_struttura            VARCHAR(16),
    descrizione_struttura       VARCHAR(255),
    settore                     VARCHAR(30),

    -- Ufficio
    ufficio_piano               VARCHAR(4),
    ufficio_numero_stanza       VARCHAR(10),
    ufficio_telefono            VARCHAR(16),

    -- Contatti
    email                       VARCHAR(128),
    email_personale             VARCHAR(128),

    -- Badge e foto
    p_badge                     VARCHAR(16),
    url_image                   VARCHAR(255),

    -- Configurazioni
    servizio_fuori_sede         SMALLINT,
    status                      VARCHAR(50),

    -- Audit fields
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    is_not_deleted              SMALLINT DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_rapporto_matricola_ente
        UNIQUE (matricola, cod_ente, is_not_deleted)
);

-- Indici per performance
CREATE INDEX IF NOT EXISTS idx_rapporto_cod_ente ON scai_rapporto(cod_ente);
CREATE INDEX IF NOT EXISTS idx_rapporto_matricola ON scai_rapporto(matricola);
CREATE INDEX IF NOT EXISTS idx_rapporto_cod_fis ON scai_rapporto(cod_fis);
CREATE INDEX IF NOT EXISTS idx_rapporto_cognome ON scai_rapporto(cognome);
CREATE INDEX IF NOT EXISTS idx_rapporto_nome ON scai_rapporto(nome);
CREATE INDEX IF NOT EXISTS idx_rapporto_data_assunzione ON scai_rapporto(data_assunzione);
CREATE INDEX IF NOT EXISTS idx_rapporto_data_cessazione ON scai_rapporto(data_cessazione);
CREATE INDEX IF NOT EXISTS idx_rapporto_status ON scai_rapporto(status);
CREATE INDEX IF NOT EXISTS idx_rapporto_is_not_deleted ON scai_rapporto(is_not_deleted);

COMMENT ON TABLE scai_rapporto IS 'Rapporti di lavoro dei dipendenti - Sistema SCAI';
COMMENT ON COLUMN scai_rapporto.cod_ente IS 'Codice ente (max 15 caratteri)';
COMMENT ON COLUMN scai_rapporto.matricola IS 'Matricola dipendente (max 15 caratteri)';
COMMENT ON COLUMN scai_rapporto.cod_rapporto IS 'Codice rapporto (max 15 caratteri)';
COMMENT ON COLUMN scai_rapporto.p_badge IS 'Badge virtuale HR';
COMMENT ON COLUMN scai_rapporto.servizio_fuori_sede IS 'Indica se il servizio fuori sede è abilitato (0=no, 1=sì)';
COMMENT ON COLUMN scai_rapporto.status IS 'Stato del rapporto: nuovo, aggiornato, chiuso_nel_passato';
COMMENT ON COLUMN scai_rapporto.is_not_deleted IS 'Soft delete: 1=attivo, NULL=cancellato';

-- ============================================================================
-- TABELLA: scai_rapporto_foto
-- ============================================================================
-- Fotografie dei dipendenti associate ai rapporti

CREATE TABLE IF NOT EXISTS scai_rapporto_foto (
    id             BIGSERIAL PRIMARY KEY,
    cod_ente       VARCHAR(15) NOT NULL,
    matricola      VARCHAR(15) NOT NULL,
    foto           BYTEA,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    is_not_deleted SMALLINT DEFAULT 1,

    CONSTRAINT uq_rapporto_foto
        UNIQUE (cod_ente, matricola, is_not_deleted)
);

CREATE INDEX IF NOT EXISTS idx_rapporto_foto_cod_ente ON scai_rapporto_foto(cod_ente);
CREATE INDEX IF NOT EXISTS idx_rapporto_foto_matricola ON scai_rapporto_foto(matricola);

COMMENT ON TABLE scai_rapporto_foto IS 'Fotografie dei dipendenti';
COMMENT ON COLUMN scai_rapporto_foto.foto IS 'Immagine in formato binario (BYTEA)';

-- ============================================================================
-- FOREIGN KEYS (opzionali)
-- ============================================================================
-- Le FK sono commentate perché il backend Laravel gestisce le relazioni
-- a livello applicativo. Decommentare se si implementa backend Java/Spring Boot.

-- Relazione con rapporto principale
-- ALTER TABLE scai_rapporto_foto
--     ADD CONSTRAINT fk_rapporto_foto_rapporto
--     FOREIGN KEY (cod_ente, matricola)
--     REFERENCES scai_rapporto(cod_ente, matricola)
--     ON DELETE CASCADE;

-- ============================================================================
-- RELAZIONI CON ALTRI MODULI
-- ============================================================================
-- Il modulo rapporti è referenziato da:
--   - modulo badge → scai_badge(cod_ente, matricola) FK scai_rapporto
--   - modulo policy → scai_policy_rapporto(cod_ente, matricola) FK scai_rapporto
--   - modulo veicoli → scai_veicolo_convenzione(cod_ente, matricola) FK scai_rapporto
--
-- Le tabelle di questi moduli verranno create dalle rispettive migration.
-- La chiave naturale (cod_ente, matricola) permette le relazioni cross-modulo.

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

DROP TRIGGER IF EXISTS update_scai_rapporto_updated_at ON scai_rapporto;
CREATE TRIGGER update_scai_rapporto_updated_at
    BEFORE UPDATE ON scai_rapporto
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_scai_rapporto_foto_updated_at ON scai_rapporto_foto;
CREATE TRIGGER update_scai_rapporto_foto_updated_at
    BEFORE UPDATE ON scai_rapporto_foto
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- NOTE SULL'ARCHITETTURA MODULARE
-- ============================================================================
-- Questo modulo contiene SOLO le tabelle:
--   - scai_rapporto (entità principale)
--   - scai_rapporto_foto (dipendenza 1:1 diretta)
--
-- Le tabelle di altre entità sono gestite da moduli separati:
--   - scai_badge → modulo "badge"
--   - scai_badge_tipo → modulo "badge"
--   - scai_badge_request → modulo "badge"
--   - scai_policy_rapporto → modulo "policy"
--   - scai_policy_rapporto_validity → modulo "policy"
--   - scai_veicolo → modulo "veicoli"
--   - scai_veicolo_convenzione → modulo "veicoli"
--
-- Questa separazione garantisce:
--   ✓ Modularità: ogni modulo è autonomo
--   ✓ Riusabilità: i moduli possono essere installati indipendentemente
--   ✓ Manutenibilità: modifiche isolate per dominio
--   ✓ No accoppiamento: nessuna dipendenza circolare

-- ============================================================================
-- CAMPAGNE
-- ============================================================================

CREATE TABLE jms_crm_campagne (
    id          SERIAL PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    descrizione TEXT,
    stato       INTEGER NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE UNIQUE INDEX jms_crm_idx_campagne_nome ON jms_crm_campagne(nome) WHERE deleted_at IS NULL;
CREATE INDEX       jms_crm_idx_campagne_stato ON jms_crm_campagne(stato);

-- ============================================================================
-- RELAZIONE CAMPAGNE-LISTE (Many-to-Many)
-- ============================================================================

CREATE TABLE jms_crm_campagna_liste (
    campagna_id INTEGER NOT NULL REFERENCES jms_crm_campagne(id) ON DELETE CASCADE,
    lista_id    INTEGER NOT NULL REFERENCES jms_crm_liste(id)    ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (campagna_id, lista_id)
);

CREATE INDEX jms_crm_idx_campagna_liste_campagna ON jms_crm_campagna_liste(campagna_id);
CREATE INDEX jms_crm_idx_campagna_liste_lista    ON jms_crm_campagna_liste(lista_id);

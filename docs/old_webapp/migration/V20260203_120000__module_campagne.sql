-- ============================================================================
-- MODULO CAMPAGNE
-- ============================================================================

CREATE TABLE IF NOT EXISTS campagne (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descrizione TEXT,
    tipo VARCHAR(50) NOT NULL DEFAULT 'outbound',
    stato INTEGER NOT NULL DEFAULT 1,
    data_inizio DATE,
    data_fine DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS campagna_liste (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    campagna_id INTEGER NOT NULL,
    lista_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (campagna_id) REFERENCES campagne(id) ON DELETE CASCADE,
    FOREIGN KEY (lista_id) REFERENCES liste(id) ON DELETE CASCADE,
    UNIQUE(campagna_id, lista_id)
);

-- Indici
CREATE INDEX IF NOT EXISTS idx_campagne_stato ON campagne(stato);
CREATE INDEX IF NOT EXISTS idx_campagne_tipo ON campagne(tipo);
CREATE INDEX IF NOT EXISTS idx_campagna_liste_campagna ON campagna_liste(campagna_id);
CREATE INDEX IF NOT EXISTS idx_campagna_liste_lista ON campagna_liste(lista_id);

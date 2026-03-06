-- ============================================================================
-- FIX SCHEMA IMPORT_SESSIONS - Ricrea tabella con struttura corretta
-- ============================================================================

-- Elimina la vecchia tabella (sicuro perché è vuota)
DROP TABLE IF EXISTS import_sessions;

-- Ricrea la tabella con la struttura corretta
CREATE TABLE import_sessions (
    id VARCHAR(36) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    row_count INTEGER NOT NULL,
    headers TEXT,
    preview TEXT,
    column_mapping TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Ricrea gli indici
CREATE INDEX IF NOT EXISTS idx_import_sessions_status ON import_sessions(status);

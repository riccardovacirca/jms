-- Tabella utenti unificata
CREATE TABLE utenti (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    ruolo VARCHAR(20) NOT NULL,  -- 'ADMIN' o 'OPERATORE'
    attivo BOOLEAN NOT NULL DEFAULT 1,

    -- Campi per operatori (NULL per ADMIN)
    nome VARCHAR(100),
    cognome VARCHAR(100),
    telefono VARCHAR(20),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella refresh tokens
CREATE TABLE refresh_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    token VARCHAR(255) NOT NULL UNIQUE,
    utente_id INTEGER NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT 0,

    FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE
);

CREATE INDEX idx_utenti_username ON utenti(username);
CREATE INDEX idx_utenti_ruolo ON utenti(ruolo);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_utente ON refresh_tokens(utente_id);

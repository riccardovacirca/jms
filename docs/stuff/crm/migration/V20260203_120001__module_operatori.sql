CREATE TABLE operatori (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    username VARCHAR(50) NOT NULL UNIQUE,
    stato_attuale VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE operatori_campagne (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operatore_id INTEGER NOT NULL,
    campagna_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (operatore_id) REFERENCES operatori(id) ON DELETE CASCADE,
    FOREIGN KEY (campagna_id) REFERENCES campagne(id) ON DELETE CASCADE,
    UNIQUE(operatore_id, campagna_id)
);

CREATE TABLE operatori_attivita (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operatore_id INTEGER NOT NULL,
    azione VARCHAR(50) NOT NULL,
    descrizione TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (operatore_id) REFERENCES operatori(id) ON DELETE CASCADE
);

CREATE TABLE operatori_stato (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operatore_id INTEGER NOT NULL,
    stato VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (operatore_id) REFERENCES operatori(id) ON DELETE CASCADE
);

CREATE INDEX idx_operatori_email ON operatori(email);
CREATE INDEX idx_operatori_stato_attuale ON operatori(stato_attuale);
CREATE INDEX idx_operatori_campagne_operatore ON operatori_campagne(operatore_id);
CREATE INDEX idx_operatori_campagne_campagna ON operatori_campagne(campagna_id);
CREATE INDEX idx_operatori_attivita_operatore ON operatori_attivita(operatore_id);
CREATE INDEX idx_operatori_stato_operatore ON operatori_stato(operatore_id);

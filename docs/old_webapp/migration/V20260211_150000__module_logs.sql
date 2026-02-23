-- Modulo LOGS: Centralizzazione log frontend/backend

CREATE TABLE logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  level VARCHAR(20) NOT NULL,           -- DEBUG, INFO, WARN, ERROR
  module VARCHAR(100) NOT NULL,         -- Nome modulo (es: 'contatti', 'liste', 'init')
  message TEXT NOT NULL,                -- Messaggio log
  data TEXT,                            -- Dati aggiuntivi in formato JSON
  user_id INTEGER,                      -- ID utente che ha generato il log (opzionale)
  session_id VARCHAR(100),              -- ID sessione (opzionale)
  ip_address VARCHAR(45),               -- Indirizzo IP (opzionale)
  user_agent TEXT,                      -- User agent browser (opzionale)
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_logs_timestamp ON logs(timestamp DESC);
CREATE INDEX idx_logs_level ON logs(level);
CREATE INDEX idx_logs_module ON logs(module);
CREATE INDEX idx_logs_user ON logs(user_id);
CREATE INDEX idx_logs_created_at ON logs(created_at DESC);

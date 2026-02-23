-- Tabella per metadati installazione
-- Ogni installazione CRM ha un installation_id univoco e un shared_secret per firmare i token

CREATE TABLE installation_metadata (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  installation_id VARCHAR(36) UNIQUE NOT NULL,
  installation_name VARCHAR(100),
  shared_secret VARCHAR(64) NOT NULL,
  cloud_webhook_url VARCHAR(255),
  is_active INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_installation_id ON installation_metadata(installation_id);

-- Inserisci installazione di default (questa istanza)
-- L'installation_id verrà generato al primo avvio se non esiste
INSERT INTO installation_metadata (installation_id, installation_name, shared_secret, is_active)
VALUES ('default', 'Default Installation', 'changeme_on_first_run', 1);

-- Modulo INIT: Entità applicative per dati aziendali e configurazione
-- Il wizard di configurazione iniziale popola queste tabelle

-- Tabella azienda (single row - dati dell'installazione)
CREATE TABLE azienda (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  ragione_sociale VARCHAR(255) NOT NULL,
  forma_giuridica VARCHAR(100),
  partita_iva VARCHAR(50),
  codice_fiscale VARCHAR(50),
  codice_sdi VARCHAR(10),
  pec VARCHAR(255),
  numero_rea VARCHAR(50),
  capitale_sociale VARCHAR(50),

  -- Sede legale
  sede_legale_indirizzo VARCHAR(255),
  sede_legale_cap VARCHAR(10),
  sede_legale_citta VARCHAR(100),
  sede_legale_provincia VARCHAR(10),
  sede_legale_nazione VARCHAR(50) DEFAULT 'Italia',

  -- Contatti principali
  telefono_generale VARCHAR(50),
  email_generale VARCHAR(255),
  sito_web VARCHAR(255),
  referente_commerciale VARCHAR(255),
  referente_tecnico VARCHAR(255),

  -- Dati fatturazione
  intestatario_fatturazione VARCHAR(255),
  indirizzo_fatturazione VARCHAR(255),
  iban VARCHAR(50),
  modalita_pagamento VARCHAR(100),
  regime_iva VARCHAR(100),

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella sedi operative (esistente come sedi ma estesa)
CREATE TABLE IF NOT EXISTS sedi (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome VARCHAR(255) NOT NULL,
  indirizzo VARCHAR(255),
  cap VARCHAR(10),
  citta VARCHAR(100),
  provincia VARCHAR(10),
  nazione VARCHAR(50) DEFAULT 'Italia',
  numero_postazioni INTEGER,
  responsabile_nome VARCHAR(255),
  telefono VARCHAR(50),
  email VARCHAR(255),
  attiva INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Nota: Usiamo la tabella 'utenti' esistente per tutti gli account
-- Non creiamo una tabella separata 'accounts'

-- Tabella struttura organizzativa (reparti, team, gerarchie)
CREATE TABLE struttura_organizzativa (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  tipo VARCHAR(50) NOT NULL,  -- 'reparto', 'team', 'gerarchia'
  nome VARCHAR(255) NOT NULL,
  descrizione TEXT,
  responsabile_id INTEGER,
  sede_id INTEGER,
  parent_id INTEGER,
  attivo INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (responsabile_id) REFERENCES accounts(id) ON DELETE SET NULL,
  FOREIGN KEY (sede_id) REFERENCES sedi(id) ON DELETE SET NULL,
  FOREIGN KEY (parent_id) REFERENCES struttura_organizzativa(id) ON DELETE SET NULL
);

CREATE INDEX idx_struttura_tipo ON struttura_organizzativa(tipo);
CREATE INDEX idx_struttura_sede ON struttura_organizzativa(sede_id);

-- Tabella configurazioni sistema (key-value per configurazioni generali)
CREATE TABLE configurazioni (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  categoria VARCHAR(100) NOT NULL,  -- 'crm', 'sicurezza', 'licenza', 'server', 'email', 'database'
  chiave VARCHAR(100) NOT NULL,
  valore TEXT,
  tipo VARCHAR(50),  -- 'string', 'integer', 'boolean', 'json'
  descrizione TEXT,
  modificabile INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(categoria, chiave)
);

CREATE INDEX idx_config_categoria ON configurazioni(categoria);
CREATE INDEX idx_config_chiave ON configurazioni(chiave);

-- Tabella integrazioni esterne
CREATE TABLE integrazioni (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  tipo VARCHAR(50) NOT NULL,  -- 'voip', 'api', 'webhook', 'pec', 'smtp'
  nome VARCHAR(255) NOT NULL,
  host VARCHAR(255),
  porta INTEGER,
  username VARCHAR(255),
  password VARCHAR(255),
  api_key VARCHAR(255),
  endpoint VARCHAR(255),
  webhook_url VARCHAR(255),
  webhook_token VARCHAR(255),
  configurazione_json TEXT,
  attivo INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_integrazioni_tipo ON integrazioni(tipo);
CREATE INDEX idx_integrazioni_attivo ON integrazioni(attivo);

-- Tabella parametri call center per sede
CREATE TABLE callcenter_config (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sede_id INTEGER NOT NULL,
  orari_operativi VARCHAR(255),
  max_chiamate_simultanee INTEGER,
  code_chiamata TEXT,
  sla_configurati TEXT,
  script_predefiniti TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sede_id) REFERENCES sedi(id) ON DELETE CASCADE
);

CREATE INDEX idx_callcenter_sede ON callcenter_config(sede_id);

-- Aggiungi flag wizard_completed a installation_metadata
-- (usa la tabella esistente per tracciare se il wizard è stato completato)
ALTER TABLE installation_metadata ADD COLUMN wizard_completed INTEGER DEFAULT 0;
ALTER TABLE installation_metadata ADD COLUMN wizard_completed_at TIMESTAMP;

-- Inserisci configurazioni di default
INSERT INTO configurazioni (categoria, chiave, valore, tipo, descrizione, modificabile) VALUES
  ('crm', 'fuso_orario', 'Europe/Rome', 'string', 'Fuso orario predefinito', 1),
  ('crm', 'lingua', 'it', 'string', 'Lingua predefinita interfaccia', 1),
  ('crm', 'orari_lavoro', '09:00-18:00', 'string', 'Orari di lavoro standard', 1),
  ('sicurezza', 'politica_password', 'min8char', 'string', 'Politica password', 1),
  ('sicurezza', 'abilita_2fa_globale', '0', 'boolean', 'Abilita 2FA per tutti gli utenti', 1),
  ('sicurezza', 'timeout_sessione', '3600', 'integer', 'Timeout sessione in secondi', 1),
  ('sicurezza', 'log_retention_days', '90', 'integer', 'Giorni di retention log', 1),
  ('sicurezza', 'cifratura_dati_sensibili', '1', 'boolean', 'Abilita cifratura dati sensibili', 1),
  ('licenza', 'max_utenti', '100', 'integer', 'Numero massimo utenti', 1),
  ('licenza', 'max_postazioni', '50', 'integer', 'Numero massimo postazioni', 1),
  ('licenza', 'tipo_piano', 'standard', 'string', 'Tipo piano licenza', 1),
  ('server', 'ambiente', 'produzione', 'string', 'Ambiente di esecuzione', 0),
  ('server', 'limite_spazio_gb', '100', 'integer', 'Limite spazio disco in GB', 1),
  ('monitoraggio', 'email_alert', '', 'string', 'Email per alert sistema', 1),
  ('monitoraggio', 'log_avanzati', '0', 'boolean', 'Abilita log avanzati', 1),
  ('monitoraggio', 'frequenza_backup', 'giornaliero', 'string', 'Frequenza backup', 1),
  ('monitoraggio', 'modalita_aggiornamenti', 'manuale', 'string', 'Modalità aggiornamenti', 1);

CREATE TABLE jms_chiamate (
  id                  BIGSERIAL     PRIMARY KEY,
  uuid                VARCHAR(64),
  conversazione_uuid  VARCHAR(64),
  direzione           VARCHAR(16),
  stato               VARCHAR(32),
  tipo_mittente       VARCHAR(16),
  numero_mittente     VARCHAR(32),
  tipo_destinatario   VARCHAR(16),
  numero_destinatario VARCHAR(32),
  tariffa             VARCHAR(16),
  costo               VARCHAR(16),
  durata              INTEGER,
  ora_inizio          TIMESTAMP,
  ora_fine            TIMESTAMP,
  rete                VARCHAR(32),
  answer_url          TEXT,
  event_url           TEXT,
  errore_titolo       VARCHAR(255),
  errore_dettaglio    TEXT,
  operatore_id        BIGINT,
  contatto_id         BIGINT,
  data_creazione      TIMESTAMP DEFAULT NOW(),
  data_aggiornamento  TIMESTAMP
);

CREATE TABLE jms_cti_operatori (
  id                  SERIAL       PRIMARY KEY,
  vonage_user_id      VARCHAR(100) NOT NULL UNIQUE,
  account_id          INTEGER,
  sessione_account_id INTEGER,
  nome                VARCHAR(100),
  attivo              BOOLEAN      NOT NULL DEFAULT TRUE,
  data_creazione      TIMESTAMP    NOT NULL DEFAULT NOW()
);

ALTER TABLE jms_cti_operatori ADD COLUMN sessione_ttl TIMESTAMP;

CREATE UNIQUE INDEX idx_jms_cti_operatori_sessione
    ON jms_cti_operatori(sessione_account_id)
    WHERE sessione_account_id IS NOT NULL;

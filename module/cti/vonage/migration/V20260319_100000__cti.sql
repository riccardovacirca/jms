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
  operatore_id          BIGINT,
  chiamante_account_id  INTEGER,
  contatto_id           BIGINT,
  data_creazione        TIMESTAMP DEFAULT NOW(),
  data_aggiornamento    TIMESTAMP
);

CREATE TABLE jms_cti_operatori (
  id               SERIAL       PRIMARY KEY,
  vonage_user_id   VARCHAR(100) NOT NULL UNIQUE,
  account_id       INTEGER,
  claim_account_id INTEGER,
  claim_scadenza   TIMESTAMP,
  nome             VARCHAR(100),
  attivo           BOOLEAN      NOT NULL DEFAULT TRUE,
  data_creazione   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX jms_idx_cti_operatori_claim
    ON jms_cti_operatori(claim_account_id)
    WHERE claim_account_id IS NOT NULL;

-- stato: 0=disconnesso, 1=connesso, 2=in pausa, 3=in chiamata
CREATE TABLE jms_sessione_operatore (
  id                   BIGSERIAL    PRIMARY KEY,
  operatore_id         INTEGER      NOT NULL,

  -- Turno pianificato (definito dall'admin)
  turno_inizio         TIMESTAMP    NOT NULL,
  turno_fine           TIMESTAMP    NOT NULL,

  -- Connessione effettiva
  connessione_inizio   TIMESTAMP,
  connessione_fine     TIMESTAMP,
  durata_totale        INTEGER,

  -- Pause (disconnessioni dentro l'orario del turno)
  numero_pause         INTEGER      NOT NULL DEFAULT 0,
  durata_pause         INTEGER      NOT NULL DEFAULT 0,

  -- Ultima connessione effettiva (aggiornata a ogni reconnessione, usata per calcolo durata pausa)
  ultima_connessione   TIMESTAMP,

  -- Statistiche chiamate
  numero_chiamate      INTEGER      NOT NULL DEFAULT 0,
  durata_conversazione INTEGER      NOT NULL DEFAULT 0,

  -- Stato corrente: 0=disconnesso, 1=connesso, 2=in pausa, 3=in chiamata
  stato                SMALLINT     NOT NULL DEFAULT 0,

  -- Note admin
  note                 TEXT,

  -- Audit
  creato_da            INTEGER      NOT NULL,
  data_creazione       TIMESTAMP    NOT NULL DEFAULT NOW(),
  modificato_da        INTEGER,
  data_modifica        TIMESTAMP
);

CREATE INDEX jms_idx_sessione_operatore_operatore ON jms_sessione_operatore(operatore_id);
CREATE INDEX jms_idx_sessione_operatore_turno     ON jms_sessione_operatore(turno_inizio, turno_fine);
CREATE INDEX jms_idx_sessione_operatore_stato     ON jms_sessione_operatore(stato);

CREATE TABLE prefissi_internazionali (
    id       SERIAL       PRIMARY KEY,
    paese    VARCHAR(100) NOT NULL,
    iso      CHAR(2)      NOT NULL,
    prefisso VARCHAR(10)  NOT NULL,
    attivo   BOOLEAN      DEFAULT TRUE,

    CONSTRAINT uq_iso UNIQUE (iso)
);

CREATE INDEX jms_idx_prefissi_paese    ON prefissi_internazionali(paese);
CREATE INDEX jms_idx_prefissi_prefisso ON prefissi_internazionali(prefisso);

INSERT INTO prefissi_internazionali (paese, iso, prefisso) VALUES
('Afghanistan',          'AF', '+93'),
('Albania',              'AL', '+355'),
('Algeria',              'DZ', '+213'),
('Andorra',              'AD', '+376'),
('Angola',               'AO', '+244'),
('Argentina',            'AR', '+54'),
('Armenia',              'AM', '+374'),
('Australia',            'AU', '+61'),
('Austria',              'AT', '+43'),
('Azerbaigian',          'AZ', '+994'),
('Bahamas',              'BS', '+1'),
('Bahrain',              'BH', '+973'),
('Bangladesh',           'BD', '+880'),
('Belgio',               'BE', '+32'),
('Brasile',              'BR', '+55'),
('Bulgaria',             'BG', '+359'),
('Canada',               'CA', '+1'),
('Cina',                 'CN', '+86'),
('Croazia',              'HR', '+385'),
('Danimarca',            'DK', '+45'),
('Egitto',               'EG', '+20'),
('Emirati Arabi Uniti',  'AE', '+971'),
('Estonia',              'EE', '+372'),
('Finlandia',            'FI', '+358'),
('Francia',              'FR', '+33'),
('Germania',             'DE', '+49'),
('Giappone',             'JP', '+81'),
('Grecia',               'GR', '+30'),
('India',                'IN', '+91'),
('Indonesia',            'ID', '+62'),
('Irlanda',              'IE', '+353'),
('Islanda',              'IS', '+354'),
('Italia',               'IT', '+39'),
('Kenya',                'KE', '+254'),
('Lussemburgo',          'LU', '+352'),
('Messico',              'MX', '+52'),
('Norvegia',             'NO', '+47'),
('Nuova Zelanda',        'NZ', '+64'),
('Paesi Bassi',          'NL', '+31'),
('Polonia',              'PL', '+48'),
('Portogallo',           'PT', '+351'),
('Qatar',                'QA', '+974'),
('Regno Unito',          'GB', '+44'),
('Romania',              'RO', '+40'),
('Russia',               'RU', '+7'),
('Spagna',               'ES', '+34'),
('Stati Uniti',          'US', '+1'),
('Sudafrica',            'ZA', '+27'),
('Svezia',               'SE', '+46'),
('Svizzera',             'CH', '+41'),
('Turchia',              'TR', '+90'),
('Ucraina',              'UA', '+380'),
('Ungheria',             'HU', '+36');

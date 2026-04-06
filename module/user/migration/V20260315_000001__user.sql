-- Ruoli gerarchici. Il livello numerico determina i privilegi (maggiore = più privilegiato).
-- guest (0) non è nel DB: è lo stato implicito non autenticato.
CREATE TABLE jms_roles (
  name  VARCHAR(10) PRIMARY KEY,
  level SMALLINT    NOT NULL
);

INSERT INTO jms_roles (name, level) VALUES
  ('user',  1),
  ('admin', 2),
  ('root',  3);

-- Tabella account. password_hash contiene "salt:hash" generato con PBKDF2WithHmacSHA256.
-- ruolo: FK a jms_roles.name.
-- attivo: se false il login e il refresh vengono rifiutati.
-- must_change_password: se true il login restituisce il flag; il client deve
--   reindirizzare al cambio password prima di accedere all'applicazione.
-- email: opzionale, unica se presente.
-- two_factor_enabled: se true e l'account ha email e Mail è configurato, il login richiede PIN via email.
CREATE TABLE jms_accounts (
  id                   SERIAL       PRIMARY KEY,
  username             VARCHAR(100) UNIQUE NOT NULL,
  password_hash        VARCHAR(255)        NOT NULL,
  ruolo                VARCHAR(10)         NOT NULL DEFAULT 'user' REFERENCES jms_roles(name),
  attivo               BOOLEAN             NOT NULL DEFAULT true,
  must_change_password BOOLEAN             NOT NULL DEFAULT false,
  two_factor_enabled   BOOLEAN             NOT NULL DEFAULT false,
  email                VARCHAR(255)        UNIQUE,
  created_at           TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Refresh token opachi conservati nel DB per consentire la revoca.
-- Eliminati in cascata se l'account viene rimosso.
CREATE TABLE jms_refresh_tokens (
  token      VARCHAR(128) PRIMARY KEY,
  account_id INTEGER      NOT NULL REFERENCES jms_accounts(id) ON DELETE CASCADE,
  expires_at TIMESTAMP    NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Token per il reset della password via email.
-- Il token viene generato al momento della richiesta e inviato via link email.
-- Il reset avviene solo se il token è valido, non scaduto e non ancora usato.
-- Eliminati in cascata se l'account viene rimosso.
CREATE TABLE jms_password_reset_tokens (
  token      VARCHAR(128) PRIMARY KEY,
  account_id INTEGER      NOT NULL REFERENCES jms_accounts(id) ON DELETE CASCADE,
  expires_at TIMESTAMP    NOT NULL,
  used       BOOLEAN      NOT NULL DEFAULT false,
  created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- PIN temporanei per autenticazione a due fattori via mail.
-- challenge_token: cookie opaco restituito al client dopo le credenziali.
-- Eliminati in cascata se l'account viene rimosso.
CREATE TABLE jms_auth_pins (
  id              BIGSERIAL    PRIMARY KEY,
  challenge_token VARCHAR(64)  NOT NULL UNIQUE,
  account_id      INTEGER      NOT NULL REFERENCES jms_accounts(id) ON DELETE CASCADE,
  pin_hash        VARCHAR(255) NOT NULL,
  expires_at      TIMESTAMP    NOT NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabella profili utente. Dati anagrafici associati all'account.
-- Eliminati in cascata se l'account viene rimosso.
CREATE TABLE jms_users
(
  id         BIGSERIAL                NOT NULL,
  account_id BIGINT                   NOT NULL,
  nome       VARCHAR(100),
  cognome    VARCHAR(100),
  nickname   VARCHAR(100),
  immagine   TEXT,
  flags      INTEGER                  NOT NULL DEFAULT 0,
  attivo     BOOLEAN                  NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  CONSTRAINT pk_jms_users      PRIMARY KEY (id),
  CONSTRAINT uq_jms_users_nick UNIQUE      (nickname),
  CONSTRAINT fk_jms_users_acct FOREIGN KEY (account_id) REFERENCES jms_accounts(id) ON DELETE CASCADE
);

-- Impostazioni chiave/valore per profilo utente.
-- Eliminate in cascata se il profilo viene rimosso.
CREATE TABLE jms_user_settings
(
  user_id BIGINT       NOT NULL,
  chiave  VARCHAR(100) NOT NULL,
  valore  TEXT,
  CONSTRAINT pk_jms_user_settings PRIMARY KEY (user_id, chiave),
  CONSTRAINT fk_jms_user_settings FOREIGN KEY (user_id) REFERENCES jms_users(id) ON DELETE CASCADE
);

CREATE INDEX jms_idx_users_account_id ON jms_users(account_id);

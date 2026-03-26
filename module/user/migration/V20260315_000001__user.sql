-- Tabella ruoli. I permessi associati sono in role_permissions.
CREATE TABLE roles (
  name  VARCHAR(50) PRIMARY KEY
);

INSERT INTO roles (name) VALUES
  ('admin'),
  ('operatore');

-- Permessi associati ai ruoli. Ogni riga rappresenta un permesso assegnato a un ruolo.
-- I permessi vengono embeddati nel JWT all'accesso e aggiornati ad ogni refresh.
CREATE TABLE role_permissions (
  role_name       VARCHAR(50) NOT NULL REFERENCES roles(name) ON DELETE CASCADE,
  permission_name VARCHAR(50) NOT NULL,
  PRIMARY KEY (role_name, permission_name)
);

INSERT INTO role_permissions (role_name, permission_name) VALUES
  ('admin',     'can_admin'),
  ('admin',     'can_write'),
  ('admin',     'can_delete'),
  ('admin',     'can_send_mail'),
  ('operatore', 'can_write');

-- Tabella account. password_hash contiene "salt:hash" generato con PBKDF2WithHmacSHA256.
-- ruolo: FK a roles.name.
-- attivo: se false il login e il refresh vengono rifiutati.
-- must_change_password: se true il login restituisce il flag; il client deve
--   reindirizzare al cambio password prima di accedere all'applicazione.
-- email: opzionale, unica se presente.
-- two_factor_enabled: se true e l'account ha email e Mail è configurato, il login richiede PIN via email.
CREATE TABLE accounts (
  id                   SERIAL       PRIMARY KEY,
  username             VARCHAR(100) UNIQUE NOT NULL,
  password_hash        VARCHAR(255)        NOT NULL,
  ruolo                VARCHAR(50)         NOT NULL DEFAULT 'operatore' REFERENCES roles(name),
  attivo               BOOLEAN             NOT NULL DEFAULT true,
  must_change_password BOOLEAN             NOT NULL DEFAULT false,
  two_factor_enabled   BOOLEAN             NOT NULL DEFAULT false,
  email                VARCHAR(255)        UNIQUE,
  created_at           TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Refresh token opachi conservati nel DB per consentire la revoca.
-- Eliminati in cascata se l'account viene rimosso.
CREATE TABLE refresh_tokens (
  token      VARCHAR(128) PRIMARY KEY,
  account_id INTEGER      NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  expires_at TIMESTAMP    NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Token per il reset della password via email.
-- Il token viene generato al momento della richiesta e inviato via link email.
-- Il reset avviene solo se il token è valido, non scaduto e non ancora usato.
-- Eliminati in cascata se l'account viene rimosso.
CREATE TABLE password_reset_tokens (
  token      VARCHAR(128) PRIMARY KEY,
  account_id INTEGER      NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  expires_at TIMESTAMP    NOT NULL,
  used       BOOLEAN      NOT NULL DEFAULT false,
  created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- PIN temporanei per autenticazione a due fattori via mail.
-- challenge_token: cookie opaco restituito al client dopo le credenziali.
-- Eliminati in cascata se l'account viene rimosso.
CREATE TABLE auth_pins (
  id              BIGSERIAL    PRIMARY KEY,
  challenge_token VARCHAR(64)  NOT NULL UNIQUE,
  account_id      INTEGER      NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  pin_hash        VARCHAR(255) NOT NULL,
  expires_at      TIMESTAMP    NOT NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabella profili utente. Dati anagrafici associati all'account.
-- Eliminati in cascata se l'account viene rimosso.
CREATE TABLE users
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
  CONSTRAINT pk_users      PRIMARY KEY (id),
  CONSTRAINT uq_users_nick UNIQUE      (nickname),
  CONSTRAINT fk_users_acct FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- Impostazioni chiave/valore per profilo utente.
-- Eliminate in cascata se il profilo viene rimosso.
CREATE TABLE user_settings
(
  user_id BIGINT       NOT NULL,
  chiave  VARCHAR(100) NOT NULL,
  valore  TEXT,
  CONSTRAINT pk_user_settings PRIMARY KEY (user_id, chiave),
  CONSTRAINT fk_user_settings FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_users_account_id ON users(account_id);

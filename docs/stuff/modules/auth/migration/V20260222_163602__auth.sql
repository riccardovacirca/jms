-- Tabella ruoli con flag di autorizzazione booleane.
-- Ogni colonna corrisponde a un permesso specifico.
-- I flag vengono embeddati nel JWT all'accesso e aggiornati ad ogni refresh.
CREATE TABLE roles (
  name        VARCHAR(50) PRIMARY KEY,
  can_admin   BOOLEAN     NOT NULL DEFAULT false,
  can_write   BOOLEAN     NOT NULL DEFAULT false,
  can_delete  BOOLEAN     NOT NULL DEFAULT false
);

INSERT INTO roles (name, can_admin, can_write, can_delete) VALUES
  ('admin',     true,  true,  true),
  ('operatore', false, true,  false);

-- Tabella utenti. password_hash contiene "salt:hash" generato con PBKDF2WithHmacSHA256.
-- ruolo: FK a roles.name.
-- attivo: se false il login e il refresh vengono rifiutati.
-- must_change_password: se true il login restituisce il flag; il client deve
--   reindirizzare al cambio password prima di accedere all'applicazione.
-- email: opzionale, unica se presente.
-- two_factor_enabled: se true e l'utente ha email e Mail è configurato, il login richiede PIN via email.
CREATE TABLE users (
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

-- Utente admin iniziale. Password temporanea: Admin@2026!
-- must_change_password = true forza il cambio alla prima autenticazione.
INSERT INTO users (username, password_hash, ruolo, must_change_password) VALUES
  ('admin', 'Y+A23vNH5ARx+BUiNlvcng==:knQfRmlW5n+oCHPy1WgB5mttMclMAFYCcMrgty9DoUk=', 'admin', true);

-- Refresh token opachi conservati nel DB per consentire la revoca.
-- Eliminati in cascata se l'utente viene rimosso.
CREATE TABLE refresh_tokens (
  token      VARCHAR(128) PRIMARY KEY,
  user_id    INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMP    NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- PIN temporanei per autenticazione a due fattori via mail.
-- challenge_token: cookie opaco restituito al client dopo le credenziali.
-- Eliminati in cascata se l'utente viene rimosso.
CREATE TABLE auth_pins (
  id              BIGSERIAL    PRIMARY KEY,
  challenge_token VARCHAR(64)  NOT NULL UNIQUE,
  user_id         INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  pin_hash        VARCHAR(255) NOT NULL,
  expires_at      TIMESTAMP    NOT NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

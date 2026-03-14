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

CREATE TABLE user_settings
(
  user_id BIGINT       NOT NULL,
  chiave  VARCHAR(100) NOT NULL,
  valore  TEXT,
  CONSTRAINT pk_user_settings PRIMARY KEY (user_id, chiave),
  CONSTRAINT fk_user_settings FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_users_account_id ON users(account_id);

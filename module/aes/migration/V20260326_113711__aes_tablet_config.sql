-- Tabella per configurazione tablet firma remota (Savino/Namirial)
-- Ogni tablet ha credenziali dedicate per autenticazione DM7/Conserva
CREATE TABLE aes_tablet_config
(
  id                BIGSERIAL PRIMARY KEY,
  account_id        BIGINT NOT NULL,
  tablet_id         VARCHAR(255) NOT NULL UNIQUE,
  tablet_name       VARCHAR(255) NOT NULL,
  tablet_app        VARCHAR(255) NOT NULL,
  tablet_department VARCHAR(255) NOT NULL,
  provider          VARCHAR(50) NOT NULL CHECK (provider IN ('savino', 'namirial')),
  endpoint          VARCHAR(500) NOT NULL,
  username          VARCHAR(255) NOT NULL,
  password          VARCHAR(255) NOT NULL,
  enabled           BOOLEAN NOT NULL DEFAULT true,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indice per ricerca per account
CREATE INDEX idx_aes_tablet_config_account_id ON aes_tablet_config(account_id);

-- Indice per ricerca per provider
CREATE INDEX idx_aes_tablet_config_provider ON aes_tablet_config(provider);

-- Commenti
COMMENT ON TABLE aes_tablet_config IS 'Configurazione tablet firma remota con credenziali per provider Savino/Namirial';
COMMENT ON COLUMN aes_tablet_config.account_id IS 'ID account proprietario (riferimento logico a users.id)';
COMMENT ON COLUMN aes_tablet_config.tablet_id IS 'ID univoco del tablet (es. tab_67cff3fcd42e1a501750b60d)';
COMMENT ON COLUMN aes_tablet_config.provider IS 'Provider firma remota: savino o namirial';
COMMENT ON COLUMN aes_tablet_config.endpoint IS 'URL base API provider (es. https://api.conserva.cloud/api/v1)';
COMMENT ON COLUMN aes_tablet_config.username IS 'Username per autenticazione dm7auth';
COMMENT ON COLUMN aes_tablet_config.password IS 'Password per autenticazione dm7auth (plain text - considerare encryption)';
COMMENT ON COLUMN aes_tablet_config.enabled IS 'Flag abilitazione tablet (disabilitare invece di eliminare per audit)';

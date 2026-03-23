-- Migration core per la tabella audit_log
-- Questa tabella è utilizzabile da tutti i moduli per il logging strutturato degli eventi

CREATE TABLE audit_log
(
  id         BIGSERIAL                PRIMARY KEY,
  timestamp  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  event      VARCHAR(50)              NOT NULL,
  user_id    INTEGER,
  username   VARCHAR(100),
  ip_address VARCHAR(45),
  user_agent TEXT,
  details    JSONB
);

CREATE INDEX idx_audit_log_user      ON audit_log (user_id, timestamp DESC);
CREATE INDEX idx_audit_log_event     ON audit_log (event, timestamp DESC);
CREATE INDEX idx_audit_log_timestamp ON audit_log (timestamp DESC);
CREATE INDEX idx_audit_log_ip        ON audit_log (ip_address, timestamp DESC);

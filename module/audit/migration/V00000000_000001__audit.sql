-- Migration audit: crea la tabella jms_audit_log
-- Utilizzabile da tutti i moduli per il logging strutturato degli eventi

CREATE TABLE jms_audit_log
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

CREATE INDEX idx_jms_audit_log_user      ON jms_audit_log (user_id, timestamp DESC);
CREATE INDEX idx_jms_audit_log_event     ON jms_audit_log (event, timestamp DESC);
CREATE INDEX idx_jms_audit_log_timestamp ON jms_audit_log (timestamp DESC);
CREATE INDEX idx_jms_audit_log_ip        ON jms_audit_log (ip_address, timestamp DESC);

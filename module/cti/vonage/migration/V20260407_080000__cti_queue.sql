-- Coda chiamate CTI condivisa tra operatori
CREATE TABLE jms_cti_coda_chiamate (
  id                   BIGSERIAL    PRIMARY KEY,
  contatto_json        JSONB        NOT NULL,
  stato                VARCHAR(20)  NOT NULL DEFAULT 'pending',
  priorita             INTEGER      NOT NULL DEFAULT 0,
  operatore_id         INTEGER,
  data_inserimento     TIMESTAMP    NOT NULL DEFAULT NOW(),
  data_assegnazione    TIMESTAMP,
  data_completamento   TIMESTAMP,
  esito                VARCHAR(50),
  note                 TEXT,
  
  CONSTRAINT chk_stato CHECK (stato IN ('pending', 'assigned', 'completed', 'failed', 'cancelled'))
);

CREATE INDEX jms_idx_cti_coda_stato          ON jms_cti_coda_chiamate(stato);
CREATE INDEX jms_idx_cti_coda_operatore      ON jms_cti_coda_chiamate(operatore_id);
CREATE INDEX jms_idx_cti_coda_data_ins       ON jms_cti_coda_chiamate(data_inserimento);
CREATE INDEX jms_idx_cti_coda_priorita_data  ON jms_cti_coda_chiamate(priorita DESC, data_inserimento ASC) WHERE stato = 'pending';

-- Contatto corrente assegnato all'operatore (dalla coda o manuale)
ALTER TABLE jms_cti_operatori 
  ADD COLUMN contatto_corrente JSONB,
  ADD COLUMN contatto_data_assegnazione TIMESTAMP;

COMMENT ON TABLE jms_cti_coda_chiamate IS 'Coda chiamate condivisa tra operatori CTI';
COMMENT ON COLUMN jms_cti_coda_chiamate.contatto_json IS 'Formato: {id, phone, callback, data: [{key, value, type}]}';
COMMENT ON COLUMN jms_cti_coda_chiamate.stato IS 'Stati: pending (in coda), assigned (assegnato), completed (completato), failed (fallito), cancelled (annullato)';
COMMENT ON COLUMN jms_cti_coda_chiamate.priorita IS 'Priorità: numeri più alti = priorità maggiore';
COMMENT ON COLUMN jms_cti_operatori.contatto_corrente IS 'Contatto attualmente assegnato all operatore (dalla coda o inserito manualmente)';

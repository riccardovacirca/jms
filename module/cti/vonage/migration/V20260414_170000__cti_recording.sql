-- Aggiunge le colonne per la registrazione audio delle conversazioni CTI.
-- recording_url:  URL Vonage del file audio (fornito dall'evento recording).
-- recording_uuid: UUID Vonage della registrazione.
-- recording_path: path locale del file .mp3 scaricato in storage.
ALTER TABLE jms_cti_chiamate
  ADD COLUMN IF NOT EXISTS recording_url  VARCHAR(500),
  ADD COLUMN IF NOT EXISTS recording_uuid VARCHAR(64),
  ADD COLUMN IF NOT EXISTS recording_path VARCHAR(500);

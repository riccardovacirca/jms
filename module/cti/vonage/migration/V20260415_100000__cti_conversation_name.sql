-- Aggiunge conversation_name a jms_cti_chiamate per supportare l'ascolto silenzioso.
-- Il nome simbolico della conversazione Vonage (es. "call-<uuid>") è necessario per
-- consentire a un admin di entrare in una Named Conversation già attiva tramite Client SDK.
ALTER TABLE jms_cti_chiamate
  ADD COLUMN IF NOT EXISTS conversation_name VARCHAR(100);

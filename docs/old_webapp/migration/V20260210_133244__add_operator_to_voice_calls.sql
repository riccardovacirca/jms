-- Add operator_id to voice_calls for multi-operator tracking
ALTER TABLE voice_calls ADD COLUMN operator_id INTEGER;

-- Add index for operator queries
CREATE INDEX idx_voice_calls_operator ON voice_calls(operator_id);

-- Add campagna_id and contatto_id for CRM integration
ALTER TABLE voice_calls ADD COLUMN campagna_id INTEGER;
ALTER TABLE voice_calls ADD COLUMN contatto_id INTEGER;

CREATE INDEX idx_voice_calls_campagna ON voice_calls(campagna_id);
CREATE INDEX idx_voice_calls_contatto ON voice_calls(contatto_id);

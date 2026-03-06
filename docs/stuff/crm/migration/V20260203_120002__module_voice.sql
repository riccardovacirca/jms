CREATE TABLE voice_calls (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    conversation_uuid VARCHAR(40),
    direction VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    from_type VARCHAR(20),
    from_number VARCHAR(20),
    to_type VARCHAR(20),
    to_number VARCHAR(20),
    rate VARCHAR(20),
    price VARCHAR(20),
    duration INTEGER,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    network VARCHAR(20),
    answer_url TEXT,
    event_url TEXT,
    error_title VARCHAR(100),
    error_detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE voice_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    call_id INTEGER NOT NULL,
    uuid VARCHAR(36),
    conversation_uuid VARCHAR(40),
    status VARCHAR(20),
    direction VARCHAR(10),
    timestamp TIMESTAMP,
    from_number VARCHAR(20),
    to_number VARCHAR(20),
    payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (call_id) REFERENCES voice_calls(id) ON DELETE CASCADE
);

CREATE INDEX idx_voice_calls_uuid ON voice_calls(uuid);
CREATE INDEX idx_voice_calls_status ON voice_calls(status);
CREATE INDEX idx_voice_calls_direction ON voice_calls(direction);
CREATE INDEX idx_voice_calls_created_at ON voice_calls(created_at);
CREATE INDEX idx_voice_events_call_id ON voice_events(call_id);
CREATE INDEX idx_voice_events_timestamp ON voice_events(timestamp);

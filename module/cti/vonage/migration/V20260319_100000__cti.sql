CREATE TABLE chiamate (
  id                BIGSERIAL     PRIMARY KEY,
  uuid              VARCHAR(64),
  conversation_uuid VARCHAR(64),
  direction         VARCHAR(16),
  status            VARCHAR(32),
  from_type         VARCHAR(16),
  from_number       VARCHAR(32),
  to_type           VARCHAR(16),
  to_number         VARCHAR(32),
  rate              VARCHAR(16),
  price             VARCHAR(16),
  duration          INTEGER,
  start_time        TIMESTAMP,
  end_time          TIMESTAMP,
  network           VARCHAR(32),
  answer_url        TEXT,
  event_url         TEXT,
  error_title       VARCHAR(255),
  error_detail      TEXT,
  operator_id       BIGINT,
  contatto_id       BIGINT,
  created_at        TIMESTAMP DEFAULT NOW(),
  updated_at        TIMESTAMP
);

CREATE TABLE cti_operators (
  id                 SERIAL       PRIMARY KEY,
  vonage_user_id     VARCHAR(100) NOT NULL UNIQUE,
  account_id         INTEGER,
  session_account_id INTEGER,
  display_name       VARCHAR(100),
  active             BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_cti_operators_session
    ON cti_operators(session_account_id)
    WHERE session_account_id IS NOT NULL;

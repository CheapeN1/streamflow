-- StreamFlow PostgreSQL Schema Initialization

CREATE TABLE IF NOT EXISTS user_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    session_id  VARCHAR(64) NOT NULL,
    action      VARCHAR(30) NOT NULL,
    product_id  BIGINT,
    metadata    JSONB,
    event_time  TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_user_events_user_id     ON user_events(user_id);
CREATE INDEX idx_user_events_product_id  ON user_events(product_id);
CREATE INDEX idx_user_events_action      ON user_events(action);
CREATE INDEX idx_user_events_event_time  ON user_events(event_time DESC);
CREATE INDEX idx_user_events_session_id  ON user_events(session_id);

CREATE TABLE IF NOT EXISTS product_window_metrics (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    window_start    TIMESTAMPTZ NOT NULL,
    window_end      TIMESTAMPTZ NOT NULL,
    window_type     VARCHAR(20) NOT NULL,
    click_count     BIGINT DEFAULT 0,
    cart_count      BIGINT DEFAULT 0,
    purchase_count  BIGINT DEFAULT 0,
    exit_count      BIGINT DEFAULT 0,
    conversion_rate DECIMAL(5,4),
    created_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE(product_id, window_start, window_type)
);

CREATE INDEX idx_pwm_product_id    ON product_window_metrics(product_id);
CREATE INDEX idx_pwm_window_start  ON product_window_metrics(window_start DESC);
CREATE INDEX idx_pwm_window_type   ON product_window_metrics(window_type);

CREATE TABLE IF NOT EXISTS session_metrics (
    session_id    VARCHAR(64) PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    start_time    TIMESTAMPTZ NOT NULL,
    end_time      TIMESTAMPTZ,
    total_events  INT DEFAULT 0,
    pages_visited INT DEFAULT 0,
    purchased     BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_session_metrics_user_id    ON session_metrics(user_id);
CREATE INDEX idx_session_metrics_start_time ON session_metrics(start_time DESC);
CREATE INDEX idx_session_metrics_purchased  ON session_metrics(purchased);

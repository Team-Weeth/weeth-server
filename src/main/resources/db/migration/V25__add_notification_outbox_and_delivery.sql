ALTER TABLE user_notification ADD COLUMN event_id VARCHAR(150) NULL;
ALTER TABLE user_notification ADD CONSTRAINT uk_user_notification_event_user UNIQUE (event_id, user_id);

CREATE TABLE notification_event (
    event_id VARCHAR(150) NOT NULL,
    club_id BIGINT NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (event_id)
);

CREATE TABLE notification_delivery (
    notification_delivery_id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(150) NOT NULL,
    user_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(255) NOT NULL,
    template_version INT NOT NULL,
    payload_data JSON NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL,
    targets_initialized BOOLEAN NOT NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    claim_token VARCHAR(36) NULL,
    lease_until DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    dead_lettered_at DATETIME(6) NULL,
    last_error_code VARCHAR(100) NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (notification_delivery_id),
    CONSTRAINT uk_delivery_event_user_channel UNIQUE (event_id, user_id, channel)
);
CREATE INDEX idx_delivery_due ON notification_delivery (status, next_attempt_at);
CREATE INDEX idx_delivery_lease ON notification_delivery (status, lease_until);

CREATE TABLE notification_outbox (
    notification_outbox_id BIGINT NOT NULL AUTO_INCREMENT,
    delivery_id BIGINT NOT NULL,
    event_id VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    claim_token VARCHAR(36) NULL,
    lease_until DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    last_error_code VARCHAR(100) NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (notification_outbox_id),
    CONSTRAINT uk_outbox_delivery UNIQUE (delivery_id),
    CONSTRAINT fk_outbox_delivery FOREIGN KEY (delivery_id) REFERENCES notification_delivery (notification_delivery_id)
);
CREATE INDEX idx_outbox_due ON notification_outbox (status, next_attempt_at);

CREATE TABLE notification_delivery_target (
    notification_delivery_target_id BIGINT NOT NULL AUTO_INCREMENT,
    delivery_id BIGINT NOT NULL,
    token_id BIGINT NOT NULL,
    registered_at DATETIME(6) NOT NULL,
    status VARCHAR(30) NOT NULL,
    last_error_code VARCHAR(100) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (notification_delivery_target_id),
    CONSTRAINT uk_delivery_target_token UNIQUE (delivery_id, token_id),
    CONSTRAINT fk_delivery_target_delivery FOREIGN KEY (delivery_id) REFERENCES notification_delivery (notification_delivery_id)
);

CREATE TABLE notification_queue_incident (
    message_id VARCHAR(100) NOT NULL,
    delivery_id BIGINT NULL,
    event_id VARCHAR(150) NULL,
    reason VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (message_id)
);

ALTER TABLE user_notification ADD COLUMN template_version INT NULL;

CREATE TABLE notification_failure (
    notification_failure_id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(150) NOT NULL,
    club_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    template_version INT NULL,
    reason VARCHAR(40) NOT NULL,
    missing_variables VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (notification_failure_id)
);

CREATE INDEX idx_notification_failure_event_created ON notification_failure (event_id, created_at);

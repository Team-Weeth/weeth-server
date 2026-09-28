CREATE TABLE notification_token (
    notification_token_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'WEB',
    is_active BOOLEAN NOT NULL,
    last_registered_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (notification_token_id),
    CONSTRAINT uk_notification_token_token UNIQUE (token),
    CONSTRAINT fk_notification_token_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX idx_notification_token_user_active
    ON notification_token (user_id, is_active);

CREATE TABLE user_notification (
    user_notification_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(255) NOT NULL,
    target_path VARCHAR(255) NOT NULL,
    club_id BIGINT NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (user_notification_id),
    CONSTRAINT fk_user_notification_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX idx_user_notification_user_club_created
    ON user_notification (user_id, club_id, created_at DESC);

CREATE INDEX idx_user_notification_user_club_read
    ON user_notification (user_id, club_id, is_read);

CREATE INDEX idx_user_notification_type_reference
    ON user_notification (type, reference_type, reference_id);

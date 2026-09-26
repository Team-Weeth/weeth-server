CREATE TABLE notification_token (
    notification_token_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
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
    type ENUM ('NOTICE_CREATED') NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(200) NOT NULL,
    target_path VARCHAR(255) NOT NULL,
    club_id BIGINT NOT NULL,
    board_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (user_notification_id),
    CONSTRAINT fk_user_notification_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX idx_user_notification_user_club_read_created
    ON user_notification (user_id, club_id, is_read, created_at DESC);

CREATE INDEX idx_user_notification_notice_post
    ON user_notification (post_id, type);

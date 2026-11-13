CREATE TABLE notification_setting (
    notification_setting_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    master_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (notification_setting_id),
    CONSTRAINT uk_notification_setting_user_club UNIQUE (user_id, club_id),
    CONSTRAINT fk_notification_setting_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_notification_setting_club FOREIGN KEY (club_id) REFERENCES club (club_id)
);

CREATE TABLE notification_preference (
    notification_preference_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (notification_preference_id),
    CONSTRAINT uk_notification_preference_user_club_category UNIQUE (user_id, club_id, category),
    CONSTRAINT fk_notification_preference_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_notification_preference_club FOREIGN KEY (club_id) REFERENCES club (club_id)
);

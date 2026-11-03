CREATE TABLE feedback (
    feedback_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,
    channel VARCHAR(10) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (feedback_id),
    CONSTRAINT fk_feedback_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX idx_feedback_club_channel_created
    ON feedback (club_id, channel, created_at);

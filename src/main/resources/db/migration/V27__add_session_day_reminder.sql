ALTER TABLE notification_delivery ADD COLUMN source_session_ids JSON NULL;
ALTER TABLE notification_delivery ADD COLUMN expires_at DATETIME(6) NULL;

CREATE TABLE notification_schedule_run (
    notification_schedule_run_id BIGINT NOT NULL AUTO_INCREMENT,
    club_id BIGINT NOT NULL,
    local_date DATE NOT NULL,
    job_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    claim_token VARCHAR(36) NULL,
    lease_until DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (notification_schedule_run_id),
    CONSTRAINT uk_schedule_run_club_date_type UNIQUE (club_id, local_date, job_type)
);

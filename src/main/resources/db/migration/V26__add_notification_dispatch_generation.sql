ALTER TABLE notification_delivery ADD COLUMN generation BIGINT NOT NULL DEFAULT 0;
ALTER TABLE notification_outbox ADD COLUMN generation BIGINT NOT NULL DEFAULT 0;

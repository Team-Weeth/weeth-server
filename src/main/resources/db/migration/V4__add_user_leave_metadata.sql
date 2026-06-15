-- [위드 탈퇴] 사용자 탈퇴 메타데이터 추가

ALTER TABLE users
    ADD COLUMN left_at DATETIME(6) NULL,
    ADD COLUMN hard_delete_after DATETIME(6) NULL;

CREATE INDEX idx_users_status_hard_delete_after
    ON users (status, hard_delete_after);

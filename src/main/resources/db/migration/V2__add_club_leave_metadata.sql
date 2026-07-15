-- [동아리 탈퇴] 탈퇴 메타데이터 추가

ALTER TABLE club_member
    ADD COLUMN left_at DATETIME(6) NULL,
    ADD COLUMN hard_delete_after DATETIME(6) NULL;

ALTER TABLE post_like
    ADD COLUMN deleted_at DATETIME(6) NULL;

CREATE INDEX idx_club_member_status_hard_delete_after
    ON club_member (member_status, hard_delete_after);

CREATE INDEX idx_post_like_deleted_at
    ON post_like (deleted_at);

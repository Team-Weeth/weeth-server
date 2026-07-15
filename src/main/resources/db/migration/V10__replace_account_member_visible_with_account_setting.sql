-- [회비] 기수별 공개 여부(account.member_visible)를 동아리 단위 회비 기능 공개 설정(account_setting)으로 전환

CREATE TABLE account_setting (
    account_setting_id BIGINT NOT NULL AUTO_INCREMENT,
    club_id            BIGINT      NOT NULL,
    member_visible     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         DATETIME(6) NULL,
    modified_at        DATETIME(6) NULL,
    PRIMARY KEY (account_setting_id),
    CONSTRAINT uk_account_setting_club UNIQUE (club_id)
);

-- 기존 회비를 운영 중인 동아리는 전부 비공개(false)로 초기화한다.
INSERT INTO account_setting (club_id, member_visible, created_at, modified_at)
SELECT DISTINCT a.club_id, FALSE, NOW(6), NOW(6)
FROM account a;

ALTER TABLE account
    DROP COLUMN member_visible;

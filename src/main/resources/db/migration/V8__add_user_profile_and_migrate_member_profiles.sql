-- [마이페이지] 멀티프로필 모델 추가 및 기존 동아리 멤버 프로필 데이터 이관

CREATE TABLE user_profile (
    user_profile_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(20) NOT NULL,
    profile_image_storage_key VARCHAR(500) NULL,
    header_image_storage_key VARCHAR(500) NULL,
    bio VARCHAR(30) NULL,
    created_at DATETIME(6) NOT NULL,
    modified_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_profile_id),
    INDEX idx_user_profile_user_id (user_id),
    CONSTRAINT fk_user_profile_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

ALTER TABLE club_member
    ADD COLUMN user_profile_id BIGINT NULL,
    ADD INDEX idx_club_member_user_profile_id (user_profile_id),
    ADD CONSTRAINT fk_club_member_user_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profile (user_profile_id);

INSERT INTO user_profile (
    user_id,
    name,
    profile_image_storage_key,
    header_image_storage_key,
    bio,
    created_at,
    modified_at
)
SELECT
    u.user_id,
    COALESCE(NULLIF(LEFT(TRIM(u.name), 20), ''), '사용자'),
    COALESCE(
        NULLIF(TRIM(representative_member.profile_image_storage_key), ''),
        latest_profile_file.storage_key
    ),
    NULL,
    NULLIF(TRIM(representative_member.bio), ''),
    NOW(6),
    NOW(6)
FROM users u
JOIN (
    SELECT DISTINCT candidate.user_id
    FROM (
        SELECT cm.user_id
        FROM club_member cm
        WHERE cm.member_status = 'ACTIVE'

        UNION

        SELECT f.owner_id AS user_id
        FROM file f
        WHERE f.owner_type = 'CLUB_MEMBER_PROFILE'
    ) candidate
) profile_target ON profile_target.user_id = u.user_id
LEFT JOIN (
    SELECT ranked_member.user_id,
           ranked_member.profile_image_storage_key,
           ranked_member.bio
    FROM (
        SELECT
            cm.user_id,
            cm.profile_image_storage_key,
            cm.bio,
            ROW_NUMBER() OVER (
                PARTITION BY cm.user_id
                ORDER BY
                    CASE
                        WHEN NULLIF(TRIM(cm.profile_image_storage_key), '') IS NOT NULL
                            OR NULLIF(TRIM(cm.bio), '') IS NOT NULL
                        THEN 0
                        ELSE 1
                    END,
                    cm.modified_at DESC,
                    cm.created_at DESC,
                    cm.club_member_id DESC
            ) AS rn
        FROM club_member cm
        WHERE cm.member_status = 'ACTIVE'
    ) ranked_member
    WHERE ranked_member.rn = 1
) representative_member ON representative_member.user_id = u.user_id
LEFT JOIN (
    SELECT ranked_file.owner_id,
           ranked_file.storage_key
    FROM (
        SELECT
            f.owner_id,
            f.storage_key,
            ROW_NUMBER() OVER (
                PARTITION BY f.owner_id
                ORDER BY f.id DESC
            ) AS rn
        FROM file f
        WHERE f.owner_type = 'CLUB_MEMBER_PROFILE'
            AND f.status = 'UPLOADED'
    ) ranked_file
    WHERE ranked_file.rn = 1
) latest_profile_file ON latest_profile_file.owner_id = u.user_id;

UPDATE club_member cm
JOIN user_profile up ON up.user_id = cm.user_id
SET cm.user_profile_id = up.user_profile_id
WHERE cm.member_status = 'ACTIVE';

UPDATE file f
JOIN user_profile up ON up.user_id = f.owner_id
SET
    f.owner_type = 'USER_PROFILE_IMAGE',
    f.owner_id = up.user_profile_id
WHERE f.owner_type = 'CLUB_MEMBER_PROFILE';

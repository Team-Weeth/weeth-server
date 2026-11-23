-- file.owner_type ENUM에 RELEASE_NOTE 값 추가 (릴리즈 노트 썸네일/본문 이미지 업로드용)
-- 네이티브 MySQL ENUM 컬럼이라 FileOwnerType에 값을 추가할 때마다 동일한 마이그레이션이 필요하다.
-- 값 목록은 FileOwnerType 선언 순서와 동일하게 유지한다.

ALTER TABLE file
    MODIFY COLUMN owner_type ENUM (
        'POST',
        'COMMENT',
        'ACCOUNT_TRANSACTION',
        'CLUB_MEMBER_PROFILE',
        'USER_PROFILE_IMAGE',
        'USER_PROFILE_HEADER',
        'CLUB_PROFILE',
        'CLUB_BACKGROUND',
        'RELEASE_NOTE'
    ) NOT NULL;

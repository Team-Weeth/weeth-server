-- file.owner_type ENUM에 ACCOUNT_TRANSACTION 값 추가
-- Hibernate(ddl-auto: update)가 생성한 네이티브 MySQL ENUM 컬럼에는 코드 enum에 추가된 값이
-- 반영되지 않아, 영수증 저장 시 "Data truncated for column 'owner_type'" 오류가 발생했다.
-- 값 목록은 FileOwnerType 선언 순서와 동일하게 유지한다.
-- 주의: 이후 FileOwnerType에 값을 추가할 때마다 동일한 마이그레이션이 필요하다.

ALTER TABLE file
    MODIFY COLUMN owner_type ENUM (
        'POST',
        'COMMENT',
        'ACCOUNT_TRANSACTION',
        'CLUB_MEMBER_PROFILE',
        'CLUB_PROFILE',
        'CLUB_BACKGROUND'
    ) NOT NULL;

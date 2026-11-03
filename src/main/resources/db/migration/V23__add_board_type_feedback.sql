-- board.type ENUM에 FEEDBACK(사용성/버그 제보 게시판) 값 추가
-- Hibernate(ddl-auto: update)가 생성한 네이티브 MySQL ENUM 컬럼에는 코드 enum에 추가된 값이
-- 반영되지 않으므로 직접 갱신한다. (V8 file.owner_type과 같은 사유)
-- 값 목록은 BoardType 선언 순서와 동일하게 유지한다.
-- FEEDBACK 게시판 행 자체는 운영자가 동아리별로 직접 생성한다 (환경별 ID를 마이그레이션에 넣지 않음).

ALTER TABLE board
    MODIFY COLUMN type ENUM (
        'ALL',
        'NOTICE',
        'GALLERY',
        'GENERAL',
        'INFORMATION',
        'FEEDBACK'
    ) NOT NULL;

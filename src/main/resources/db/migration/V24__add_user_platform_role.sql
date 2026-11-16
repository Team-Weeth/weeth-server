-- 사용자 플랫폼 전역 권한. DEVELOPER만 릴리즈 노트 백오피스 접근 가능, 일반 사용자는 NULL.
-- 부여는 DB 직접 지정 또는 서버 설정으로 한다(화면 부여 기능은 1차 범위 아님).

ALTER TABLE users
    ADD COLUMN platform_role VARCHAR(20) NULL;

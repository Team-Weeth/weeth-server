-- 게시판 개수 상한을 club 단위로 관리한다.
-- 기존에는 ManageBoardUseCase의 상수(4)라 요금제별 차등이 불가능했다.
-- 기본값은 기존 동작과 동일한 4로 두어 회귀가 없게 한다.
ALTER TABLE club
    ADD COLUMN max_board_count INT NOT NULL DEFAULT 4;

-- 운영 leets club은 V3 마이그레이션(WTH-508)으로 활성 게시판이 5개가 되어
-- 현재 상한을 초과한 상태다. 게시판을 추가할 수 없으므로 여유를 두어 조정한다.
-- 파트별 분리(BE/FE/D/PM)를 고려하면 12개가 필요하다.
UPDATE club
SET max_board_count = 12
WHERE club_id = 883010121028214452;

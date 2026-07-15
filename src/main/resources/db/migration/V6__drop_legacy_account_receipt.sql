-- [회비] 레거시 정리: 죽은 잔액 필드와 Receipt 테이블 폐기.
-- total_amount/current_amount 는 신규 위저드 플로우에서 쓰이지 않고(목표액은 sum(TARGETED.due_amount) live 계산),
-- 잔액은 current_balance 단일 필드로 통합되었다. Receipt 흐름은 AccountTransaction/file 도메인으로 대체됨.
-- (회비 관련 운영 데이터가 없어 데이터 백필 없이 제거)

ALTER TABLE account
    DROP COLUMN total_amount,
    DROP COLUMN current_amount;

DROP TABLE IF EXISTS receipt;

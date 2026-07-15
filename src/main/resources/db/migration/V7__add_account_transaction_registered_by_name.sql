-- 부원 거래 상세의 "등록자" 표기를 위한 이름 스냅샷.
-- 기존 거래는 값이 없을 수 있으므로 nullable 로 추가하고 응답에서 "운영진"으로 fallback 한다.
ALTER TABLE account_transaction
    ADD COLUMN registered_by_name VARCHAR(50) NULL;

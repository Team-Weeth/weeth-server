-- [회비] 납부 대상 환불 지원 컬럼 추가

ALTER TABLE account_payment_target
    ADD COLUMN refunded_at DATETIME(6) NULL,
    ADD COLUMN refunded_by BIGINT NULL;

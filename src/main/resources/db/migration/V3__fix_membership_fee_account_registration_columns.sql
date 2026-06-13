-- [회비] 등록 플로우 컬럼 보정

ALTER TABLE account
    ADD COLUMN carry_over_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN registration_step VARCHAR(20) NOT NULL DEFAULT 'BASIC',
    ADD COLUMN last_modified_by BIGINT NULL,
    MODIFY COLUMN account_holder VARCHAR(30) NULL,
    MODIFY COLUMN bank_guide VARCHAR(30) NULL;

UPDATE account
SET registration_step = 'REVIEW'
WHERE status = 'ACTIVE';

ALTER TABLE account_transaction
    ADD COLUMN balance_after INT NULL AFTER transacted_at;

-- 적용 순서를 복원할 별도 컬럼이 없어 백필은 거래일(transacted_at)을 source of truth 로 삼는다.
-- 소급 등록으로 실제 적용 순서와 거래일 순서가 달랐던 과거 데이터는 거래일 순 누적 잔액으로 보정된다.
UPDATE account_transaction target
JOIN (
    SELECT
        account_transaction_id,
        SUM(
            CASE direction
                WHEN 'INCOME' THEN amount
                WHEN 'EXPENSE' THEN -amount
                ELSE 0
            END
        ) OVER (
            PARTITION BY account_id
            ORDER BY transacted_at ASC, account_transaction_id ASC
        ) AS calculated_balance_after
    FROM account_transaction
    WHERE deleted_at IS NULL
      AND is_applied = TRUE
) running_balance
    ON target.account_transaction_id = running_balance.account_transaction_id
SET target.balance_after = running_balance.calculated_balance_after;

UPDATE account_transaction
SET balance_after = 0
WHERE balance_after IS NULL;

ALTER TABLE account_transaction
    MODIFY COLUMN balance_after INT NOT NULL;

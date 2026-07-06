ALTER TABLE account_transaction
    ADD COLUMN balance_after INT NULL AFTER transacted_at;

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

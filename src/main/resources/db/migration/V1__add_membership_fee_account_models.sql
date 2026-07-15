-- [회비] account 도메인 1단계 모델 추가

ALTER TABLE account
    ADD COLUMN name VARCHAR(100) NULL,
    ADD COLUMN dues_amount INT NOT NULL DEFAULT 0,
    ADD COLUMN carry_over_amount INT NOT NULL DEFAULT 0,
    ADD COLUMN carry_over_memo VARCHAR(200) NULL,
    ADD COLUMN current_balance INT NOT NULL DEFAULT 0,
    ADD COLUMN bank_name VARCHAR(30) NULL,
    ADD COLUMN bank_account_number VARCHAR(50) NULL,
    ADD COLUMN account_holder VARCHAR(50) NULL,
    ADD COLUMN bank_guide VARCHAR(200) NULL,
    ADD COLUMN bank_account_visible BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN member_visible BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE account
SET name = description,
    dues_amount = total_amount,
    current_balance = current_amount,
    status = 'ACTIVE'
WHERE name IS NULL;

CREATE UNIQUE INDEX uk_account_club_cardinal
    ON account (club_id, cardinal);

CREATE TABLE account_payment_target (
    account_payment_target_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    club_member_id BIGINT NOT NULL,
    target_status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    due_amount INT NOT NULL,
    paid_amount INT NOT NULL,
    paid_at DATETIME(6) NULL,
    confirmed_by BIGINT NULL,
    memo VARCHAR(200) NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (account_payment_target_id),
    CONSTRAINT uk_account_payment_target_account_member
        UNIQUE (account_id, club_member_id),
    CONSTRAINT fk_account_payment_target_account
        FOREIGN KEY (account_id) REFERENCES account (account_id),
    CONSTRAINT fk_account_payment_target_club_member
        FOREIGN KEY (club_member_id) REFERENCES club_member (club_member_id)
);

CREATE INDEX idx_account_payment_target_account_status
    ON account_payment_target (account_id, target_status, payment_status);

CREATE TABLE account_transaction (
    account_transaction_id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    account_payment_target_id BIGINT NULL,
    type VARCHAR(20) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    source VARCHAR(50) NULL,
    amount INT NOT NULL,
    transacted_at DATETIME(6) NOT NULL,
    category VARCHAR(30) NULL,
    memo VARCHAR(200) NULL,
    deleted_at DATETIME(6) NULL,
    is_applied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (account_transaction_id),
    CONSTRAINT fk_account_transaction_account
        FOREIGN KEY (account_id) REFERENCES account (account_id),
    CONSTRAINT fk_account_transaction_payment_target
        FOREIGN KEY (account_payment_target_id)
        REFERENCES account_payment_target (account_payment_target_id)
);

CREATE INDEX idx_account_transaction_account_type_transacted_id
    ON account_transaction (account_id, type, transacted_at DESC, account_transaction_id DESC);

CREATE INDEX idx_account_transaction_account_transacted_id
    ON account_transaction (account_id, transacted_at DESC, account_transaction_id DESC);

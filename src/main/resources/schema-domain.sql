CREATE TABLE IF NOT EXISTS daily_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tx_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    tx_timestamp TIMESTAMP NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    channel VARCHAR(32),
    anomaly BOOLEAN NOT NULL DEFAULT FALSE,
    anomaly_reason VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS daily_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_date DATE NOT NULL,
    total_count INT NOT NULL,
    anomaly_count INT NOT NULL,
    total_amount NUMERIC(18,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id VARCHAR(64) UNIQUE NOT NULL,
    account_type VARCHAR(16) NOT NULL,
    balance NUMERIC(18,2) NOT NULL,
    annual_rate NUMERIC(10,6) NOT NULL
);

CREATE TABLE IF NOT EXISTS interest_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    quarter VARCHAR(8) NOT NULL,
    interest_applied NUMERIC(18,2) NOT NULL,
    new_balance NUMERIC(18,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS annual_statements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    statement_year INT NOT NULL,
    total_credits NUMERIC(18,2) NOT NULL,
    total_debits NUMERIC(18,2) NOT NULL,
    ending_balance NUMERIC(18,2) NOT NULL,
    audit_flag BOOLEAN NOT NULL DEFAULT FALSE,
    audit_note VARCHAR(255)
);

CREATE TABLE connections (
    id TEXT PRIMARY KEY,
    provider TEXT NOT NULL DEFAULT 'pluggy',
    external_item_id TEXT,
    connector_id TEXT,
    display_name TEXT NOT NULL,
    status TEXT,
    execution_status TEXT,
    last_synced_at TEXT,
    last_success_at TEXT,
    last_error_code TEXT,
    last_error_message TEXT,
    raw_payload TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO connections (
    id,
    provider,
    external_item_id,
    connector_id,
    display_name,
    status,
    execution_status,
    last_synced_at,
    last_success_at,
    last_error_code,
    last_error_message,
    created_at,
    updated_at
)
SELECT
    id,
    provider,
    external_item_id,
    connector_id,
    display_name,
    status,
    execution_status,
    last_synced_at,
    last_success_at,
    last_error_code,
    last_error_message,
    created_at,
    updated_at
FROM institutions;

CREATE UNIQUE INDEX ux_connections_provider_external_item_id
    ON connections(provider, external_item_id)
    WHERE external_item_id IS NOT NULL;

CREATE INDEX idx_connections_status ON connections(status);

ALTER TABLE accounts ADD COLUMN connection_id TEXT REFERENCES connections(id);
UPDATE accounts SET connection_id = institution_id WHERE institution_id IS NOT NULL;
CREATE INDEX idx_accounts_connection_id ON accounts(connection_id);

ALTER TABLE transactions ADD COLUMN connection_id TEXT REFERENCES connections(id);
UPDATE transactions SET connection_id = institution_id WHERE institution_id IS NOT NULL;
CREATE INDEX idx_transactions_connection_date ON transactions(connection_id, date);

ALTER TABLE sync_jobs ADD COLUMN connection_id TEXT REFERENCES connections(id);
UPDATE sync_jobs SET connection_id = institution_id WHERE institution_id IS NOT NULL;
CREATE INDEX idx_sync_jobs_connection_id ON sync_jobs(connection_id);

CREATE TABLE investments (
    id TEXT PRIMARY KEY,
    connection_id TEXT REFERENCES connections(id),
    account_id TEXT REFERENCES accounts(id),
    external_investment_id TEXT,
    type TEXT,
    subtype TEXT,
    name TEXT,
    code TEXT,
    isin TEXT,
    quantity NUMERIC,
    value NUMERIC,
    amount NUMERIC,
    currency_code TEXT NOT NULL DEFAULT 'BRL',
    date TEXT,
    raw_payload TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_investments_external_investment_id
    ON investments(external_investment_id)
    WHERE external_investment_id IS NOT NULL;

CREATE INDEX idx_investments_connection_id ON investments(connection_id);
CREATE INDEX idx_investments_account_id ON investments(account_id);

CREATE TABLE sync_events (
    id TEXT PRIMARY KEY,
    sync_job_id TEXT REFERENCES sync_jobs(id),
    connection_id TEXT REFERENCES connections(id),
    level TEXT NOT NULL,
    message TEXT NOT NULL,
    metadata TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sync_events_sync_job_id ON sync_events(sync_job_id);
CREATE INDEX idx_sync_events_connection_id ON sync_events(connection_id);

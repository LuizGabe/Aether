CREATE TABLE institutions (
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
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_institutions_provider_external_item_id
    ON institutions(provider, external_item_id)
    WHERE external_item_id IS NOT NULL;

CREATE INDEX idx_institutions_status ON institutions(status);

CREATE TABLE categories (
    id TEXT PRIMARY KEY,
    parent_id TEXT REFERENCES categories(id),
    name TEXT NOT NULL,
    kind TEXT NOT NULL,
    color TEXT,
    icon TEXT,
    system INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);

CREATE TABLE accounts (
    id TEXT PRIMARY KEY,
    institution_id TEXT REFERENCES institutions(id),
    external_account_id TEXT,
    type TEXT,
    subtype TEXT,
    name TEXT,
    marketing_name TEXT,
    owner TEXT,
    number_masked TEXT,
    currency_code TEXT NOT NULL DEFAULT 'BRL',
    balance NUMERIC NOT NULL DEFAULT 0,
    balance_date TEXT,
    credit_limit NUMERIC,
    available_credit_limit NUMERIC,
    hidden INTEGER NOT NULL DEFAULT 0,
    raw_payload TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_accounts_external_account_id
    ON accounts(external_account_id)
    WHERE external_account_id IS NOT NULL;

CREATE INDEX idx_accounts_institution_id ON accounts(institution_id);

CREATE TABLE transactions (
    id TEXT PRIMARY KEY,
    account_id TEXT REFERENCES accounts(id),
    institution_id TEXT REFERENCES institutions(id),
    external_transaction_id TEXT,
    date TEXT,
    description TEXT,
    description_raw TEXT,
    amount NUMERIC NOT NULL DEFAULT 0,
    amount_in_account_currency NUMERIC,
    currency_code TEXT NOT NULL DEFAULT 'BRL',
    balance NUMERIC,
    type TEXT,
    status TEXT,
    provider_code TEXT,
    pluggy_category TEXT,
    category_id TEXT REFERENCES categories(id),
    categorization_source TEXT,
    notes TEXT,
    ignored INTEGER NOT NULL DEFAULT 0,
    raw_payload TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_transactions_external_transaction_id
    ON transactions(external_transaction_id)
    WHERE external_transaction_id IS NOT NULL;

CREATE INDEX idx_transactions_account_date ON transactions(account_id, date);
CREATE INDEX idx_transactions_institution_date ON transactions(institution_id, date);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);
CREATE INDEX idx_transactions_description ON transactions(description);

CREATE TABLE sync_jobs (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    status TEXT NOT NULL,
    institution_id TEXT REFERENCES institutions(id),
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    scheduled_for TEXT,
    started_at TEXT,
    finished_at TEXT,
    error_code TEXT,
    error_message TEXT,
    metadata TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sync_jobs_status ON sync_jobs(status);
CREATE INDEX idx_sync_jobs_institution_id ON sync_jobs(institution_id);

CREATE TABLE app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE transactions ADD COLUMN financial_kind_override TEXT;
ALTER TABLE categories ADD COLUMN archived INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN display_name TEXT;
ALTER TABLE accounts ADD COLUMN payment_account_id TEXT REFERENCES accounts(id);

CREATE TABLE classification_rules (
    id TEXT PRIMARY KEY,
    pattern TEXT NOT NULL,
    category_id TEXT REFERENCES categories(id),
    financial_kind TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE card_bills (
    id TEXT PRIMARY KEY,
    account_id TEXT NOT NULL REFERENCES accounts(id),
    due_date TEXT,
    closing_date TEXT,
    amount NUMERIC,
    currency_code TEXT NOT NULL DEFAULT 'BRL',
    minimum_payment NUMERIC,
    raw_payload TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_card_bills_account_due ON card_bills(account_id, due_date);

CREATE TABLE investment_movements (
    id TEXT PRIMARY KEY,
    investment_id TEXT NOT NULL REFERENCES investments(id),
    date TEXT,
    type TEXT,
    description TEXT,
    amount NUMERIC,
    net_amount NUMERIC,
    quantity NUMERIC,
    raw_payload TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_investment_movements_date ON investment_movements(investment_id, date);

CREATE TABLE investment_snapshots (
    investment_id TEXT NOT NULL REFERENCES investments(id),
    snapshot_date TEXT NOT NULL,
    source_date TEXT,
    gross_amount NUMERIC,
    net_balance NUMERIC,
    currency_code TEXT NOT NULL DEFAULT 'BRL',
    PRIMARY KEY (investment_id, snapshot_date)
);

CREATE TABLE wealth_snapshots (
    snapshot_date TEXT PRIMARY KEY,
    available NUMERIC NOT NULL,
    invested NUMERIC NOT NULL,
    card_balance NUMERIC NOT NULL,
    net_worth NUMERIC NOT NULL,
    incomplete_positions INTEGER NOT NULL DEFAULT 0
);

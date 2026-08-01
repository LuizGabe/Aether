-- =============================================================================
-- PLUGGY API - Schema de integração local (finanças pessoais)
-- Gerado a partir das especificações da Pluggy API (MCP)
-- Cobre: Connectors, Items, Accounts, Transactions, Investments,
--        Identity, Bills, Categories
-- Removido: Loans, Payment Requests, Payment Intents, Payment Customers,
--           Payment Recipients, Payment Institutions, Smart Transfers, Webhooks
-- =============================================================================

-- =============================================================================
-- CONNECTORS
-- Endpoint: GET /connectors, GET /connectors/{id}
-- Entidade raiz que representa uma instituição financeira conectável.
-- =============================================================================
CREATE TABLE connectors (
    id                  INTEGER         PRIMARY KEY,
    name                VARCHAR(200)    NOT NULL,
    institutionUrl      VARCHAR(500),
    imageUrl            VARCHAR(500),
    primaryColor        VARCHAR(20),
    type                VARCHAR(100),
    country             VARCHAR(10),
    hasMFA              BOOLEAN,
    oauth               BOOLEAN,
    oauthUrl            VARCHAR(500),
    resetPasswordUrl    VARCHAR(500),
    isOpenFinance       BOOLEAN,
    health_status       VARCHAR(20),
    health_stage        VARCHAR(100),
    createdAt           TIMESTAMPTZ,
    updatedAt           TIMESTAMPTZ
);

CREATE TABLE connector_products (
    connector_id    INTEGER         NOT NULL REFERENCES connectors(id) ON DELETE CASCADE,
    product         VARCHAR(50)     NOT NULL,
    PRIMARY KEY (connector_id, product)
);

-- =============================================================================
-- ITEMS
-- =============================================================================
CREATE TABLE items (
    id                              UUID            PRIMARY KEY,
    connector_id                    INTEGER         REFERENCES connectors(id),
    status                          VARCHAR(50)     NOT NULL,
    executionStatus                 VARCHAR(50)     NOT NULL,
    webhookUrl                      VARCHAR(500),
    error_code                      VARCHAR(100),
    error_message                   TEXT,
    error_providerMessage           TEXT,
    userAction_instructions         TEXT,
    userAction_expiresAt            TIMESTAMPTZ,
    consecutiveFailedLoginAttempts  INTEGER         DEFAULT 0,
    nextAutoSyncAt                  TIMESTAMPTZ,
    consentExpiresAt                TIMESTAMPTZ,
    lastUpdatedAt                   TIMESTAMPTZ,
    createdAt                       TIMESTAMPTZ     NOT NULL,
    updatedAt                       TIMESTAMPTZ     NOT NULL
);

CREATE TABLE item_products (
    item_id     UUID        NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    product     VARCHAR(50) NOT NULL,
    PRIMARY KEY (item_id, product)
);

CREATE TABLE item_status_detail (
    item_id         UUID        NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    product         VARCHAR(50) NOT NULL,
    isUpdated       BOOLEAN,
    lastUpdatedAt   TIMESTAMPTZ,
    PRIMARY KEY (item_id, product)
);

CREATE TABLE item_status_detail_warnings (
    id              SERIAL      PRIMARY KEY,
    item_id         UUID        NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    product         VARCHAR(50) NOT NULL,
    code            VARCHAR(100),
    message         TEXT,
    providerMessage TEXT
);

-- =============================================================================
-- CATEGORIES
-- =============================================================================
CREATE TABLE categories (
    id                      VARCHAR(20)     PRIMARY KEY,
    description             VARCHAR(200)    NOT NULL,
    descriptionTranslated   VARCHAR(200),
    parentId                VARCHAR(20)     REFERENCES categories(id),
    parentDescription       VARCHAR(200)
);

-- =============================================================================
-- ACCOUNTS
-- =============================================================================
CREATE TABLE accounts (
    id              UUID            PRIMARY KEY,
    itemId          UUID            NOT NULL REFERENCES items(id),
    type            VARCHAR(20)     NOT NULL,
    subtype         VARCHAR(30)     NOT NULL,
    number          VARCHAR(100),
    name            VARCHAR(200)    NOT NULL,
    marketingName   VARCHAR(200),
    balance         NUMERIC(18,2)   NOT NULL,
    taxNumber       VARCHAR(30),
    owner           VARCHAR(200),
    currencyCode    VARCHAR(10)     NOT NULL,
    bankData_transferNumber                 VARCHAR(50),
    bankData_closingBalance                 NUMERIC(18,2),
    bankData_automaticallyInvestedBalance   NUMERIC(18,2),
    bankData_overdraftContractedLimit       NUMERIC(18,2),
    bankData_overdraftUsedLimit             NUMERIC(18,2),
    bankData_unarrangedOverdraftAmount      NUMERIC(18,2),
    bankData_hasReservedBalance             BOOLEAN,
    creditData_level                        VARCHAR(50),
    creditData_brand                        VARCHAR(50),
    creditData_brandAdditionalInfo          VARCHAR(200),
    creditData_balanceCloseDate             TIMESTAMPTZ,
    creditData_balanceDueDate               TIMESTAMPTZ,
    creditData_availableCreditLimit         NUMERIC(18,2),
    creditData_balanceForeignCurrency       NUMERIC(18,2),
    creditData_minimumPayment               NUMERIC(18,2),
    creditData_creditLimit                  NUMERIC(18,2),
    creditData_isLimitFlexible              BOOLEAN,
    creditData_status                       VARCHAR(20),
    creditData_holderType                   VARCHAR(20),
    createdAt   TIMESTAMPTZ     NOT NULL,
    updatedAt   TIMESTAMPTZ     NOT NULL
);

CREATE TABLE account_reserved_balances (
    id              SERIAL          PRIMARY KEY,
    account_id      UUID            NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name            VARCHAR(200),
    identification  VARCHAR(200)    NOT NULL
);

CREATE TABLE account_reserved_balance_amounts (
    id                      SERIAL          PRIMARY KEY,
    reserved_balance_id     INTEGER         NOT NULL REFERENCES account_reserved_balances(id) ON DELETE CASCADE,
    amount                  NUMERIC(18,2)   NOT NULL,
    currencyCode            VARCHAR(10)     NOT NULL,
    remuneration_preFixedRate               NUMERIC(10,6),
    remuneration_postFixedIndexerPercentage NUMERIC(10,6),
    remuneration_rateType                   VARCHAR(20),
    remuneration_indexer                    VARCHAR(50),
    remuneration_calculation                VARCHAR(30),
    remuneration_ratePeriodicity            VARCHAR(20),
    remuneration_indexerAdditionalInfo      VARCHAR(200)
);

CREATE TABLE account_additional_cards (
    id          SERIAL      PRIMARY KEY,
    account_id  UUID        NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    number      VARCHAR(50) NOT NULL
);

CREATE TABLE account_disaggregated_credit_limits (
    id                                  SERIAL          PRIMARY KEY,
    account_id                          UUID            NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    creditLineLimitType                 VARCHAR(100),
    consolidationType                   VARCHAR(100),
    identificationNumber                VARCHAR(100)    NOT NULL,
    isLimitFlexible                     BOOLEAN         NOT NULL,
    usedAmount                          NUMERIC(18,2)   NOT NULL,
    usedAmountCurrencyCode              VARCHAR(10)     NOT NULL,
    lineName                            VARCHAR(60),
    lineNameAdditionalInfo              VARCHAR(200),
    limitAmount                         NUMERIC(18,2),
    limitAmountCurrencyCode             VARCHAR(10),
    limitAmountReason                   VARCHAR(300),
    customizedLimitAmount               NUMERIC(18,2),
    customizedLimitAmountCurrencyCode   VARCHAR(10),
    availableAmount                     NUMERIC(18,2),
    availableAmountCurrencyCode         VARCHAR(10)
);

-- =============================================================================
-- TRANSACTIONS
-- =============================================================================
CREATE TABLE transactions (
    id                          UUID            PRIMARY KEY,
    accountId                   UUID            NOT NULL REFERENCES accounts(id),
    description                 TEXT            NOT NULL,
    descriptionRaw              TEXT,
    currencyCode                VARCHAR(10)     NOT NULL,
    amount                      NUMERIC(18,2)   NOT NULL,
    amountInAccountCurrency     NUMERIC(18,2),
    date                        TIMESTAMPTZ     NOT NULL,
    type                        VARCHAR(10)     NOT NULL,
    balance                     NUMERIC(18,2),
    providerCode                VARCHAR(200),
    status                      VARCHAR(10)     NOT NULL,
    category                    VARCHAR(200),
    categoryId                  VARCHAR(20)     REFERENCES categories(id),
    operationType               VARCHAR(100),
    operationTypeAdditionalInfo VARCHAR(300),
    providerId                  VARCHAR(200),
    "order"                     INTEGER,
    merchant_name               VARCHAR(200),
    merchant_businessName       VARCHAR(200),
    merchant_cnpj               VARCHAR(20),
    merchant_cnae               VARCHAR(20),
    cc_installmentNumber        INTEGER,
    cc_totalInstallments        INTEGER,
    cc_totalAmount              NUMERIC(18,2),
    cc_feeType                  VARCHAR(50),
    cc_feeTypeAdditionalInfo    VARCHAR(200),
    cc_otherCreditsType         VARCHAR(50),
    cc_otherCreditsAdditionalInfo VARCHAR(200),
    cc_purchaseDate             TIMESTAMPTZ,
    cc_payeeMCC                 VARCHAR(10),
    cc_cardNumber               VARCHAR(30),
    cc_billId                   UUID,
    cc_billForecastDate         VARCHAR(7),
    pd_payer_name               VARCHAR(200),
    pd_payer_documentType       VARCHAR(10),
    pd_payer_documentValue      VARCHAR(30),
    pd_payer_accountNumber      VARCHAR(50),
    pd_payer_branchNumber       VARCHAR(20),
    pd_payer_routingNumber      VARCHAR(20),
    pd_payer_routingNumberISPB  VARCHAR(20),
    pd_receiver_name            VARCHAR(200),
    pd_receiver_documentType    VARCHAR(10),
    pd_receiver_documentValue   VARCHAR(30),
    pd_receiver_accountNumber   VARCHAR(50),
    pd_receiver_branchNumber    VARCHAR(20),
    pd_receiver_routingNumber   VARCHAR(20),
    pd_receiver_routingNumberISPB VARCHAR(20),
    pd_reason                   VARCHAR(500),
    pd_referenceNumber          VARCHAR(100),
    pd_receiverReferenceId      VARCHAR(200),
    pd_authenticationCode       VARCHAR(200),
    pd_paymentMethod            VARCHAR(20),
    pd_boleto_digitableLine     VARCHAR(200),
    pd_boleto_barcode           VARCHAR(200),
    pd_boleto_baseAmount        NUMERIC(18,2),
    pd_boleto_interestAmount    NUMERIC(18,2),
    pd_boleto_penaltyAmount     NUMERIC(18,2),
    pd_boleto_discountAmount    NUMERIC(18,2),
    createdAt                   TIMESTAMPTZ     NOT NULL,
    updatedAt                   TIMESTAMPTZ     NOT NULL
);

-- =============================================================================
-- INVESTMENTS
-- =============================================================================
CREATE TABLE investments (
    id                      UUID            PRIMARY KEY,
    itemId                  UUID            NOT NULL REFERENCES items(id),
    type                    VARCHAR(20)     NOT NULL,
    subtype                 VARCHAR(30),
    code                    VARCHAR(100),
    isin                    VARCHAR(12),
    number                  VARCHAR(100),
    name                    VARCHAR(300)    NOT NULL,
    balance                 NUMERIC(18,2)   NOT NULL,
    currencyCode            VARCHAR(10)     NOT NULL,
    value                   NUMERIC(18,6),
    quantity                NUMERIC(18,6),
    amount                  NUMERIC(18,2),
    taxes                   NUMERIC(18,2),
    taxes2                  NUMERIC(18,2),
    date                    TIMESTAMPTZ     NOT NULL,
    owner                   VARCHAR(200),
    amountProfit            NUMERIC(18,2),
    amountWithdrawal        NUMERIC(18,2),
    amountOriginal          NUMERIC(18,2),
    status                  VARCHAR(20),
    dueDate                 TIMESTAMPTZ,
    issuer                  VARCHAR(200),
    issuerCNPJ              VARCHAR(20),
    issueDate               TIMESTAMPTZ,
    purchaseDate            TIMESTAMPTZ,
    gracePeriodDate         TIMESTAMPTZ,
    rate                    NUMERIC(10,6),
    rateType                VARCHAR(50),
    fixedAnnualRate         NUMERIC(10,6),
    taxExempt               BOOLEAN,
    ratePeriodicity         VARCHAR(20),
    indexerAdditionalInfo   VARCHAR(200),
    priceFactor             NUMERIC(10,6),
    lastMonthRate           NUMERIC(10,6),
    lastTwelveMonthsRate    NUMERIC(10,6),
    annualRate              NUMERIC(10,6),
    debtor_name             VARCHAR(200),
    coupon_hasCoupon        BOOLEAN,
    coupon_periodicity      VARCHAR(20),
    coupon_additionalInfo   VARCHAR(300),
    institution_name        VARCHAR(200),
    institution_number      VARCHAR(30),
    metadata_taxRegime              VARCHAR(100),
    metadata_proposalNumber         VARCHAR(100),
    metadata_processNumber          VARCHAR(100),
    metadata_fundName               VARCHAR(200),
    metadata_insurer_name           VARCHAR(200),
    metadata_insurer_cnpj           VARCHAR(20),
    metadata_anbimaClass            VARCHAR(100),
    metadata_anbimaSubclass         VARCHAR(100),
    metadata_anbimaCategory         VARCHAR(50)
);

-- =============================================================================
-- INVESTMENT TRANSACTIONS
-- =============================================================================
CREATE TABLE investment_transactions (
    id                      UUID            PRIMARY KEY,
    investment_id           UUID            NOT NULL REFERENCES investments(id) ON DELETE CASCADE,
    type                    VARCHAR(20)     NOT NULL,
    movementType            VARCHAR(10),
    quantity                NUMERIC(18,6),
    value                   NUMERIC(18,6),
    amount                  NUMERIC(18,2),
    agreedRate              NUMERIC(10,6),
    indexerPercentage       NUMERIC(10,6),
    priceFactor             NUMERIC(10,6),
    date                    TIMESTAMPTZ     NOT NULL,
    tradeDate               TIMESTAMPTZ,
    description             TEXT,
    netAmount               NUMERIC(18,2),
    brokerageNumber         VARCHAR(50),
    expenses_serviceTax             NUMERIC(18,2),
    expenses_brokerageFee           NUMERIC(18,2),
    expenses_incomeTax              NUMERIC(18,2),
    expenses_tradingAssetsNoticeFee NUMERIC(18,2),
    expenses_maintenanceFee         NUMERIC(18,2),
    expenses_settlementFee          NUMERIC(18,2),
    expenses_clearingFee            NUMERIC(18,2),
    expenses_stockExchangeFee       NUMERIC(18,2),
    expenses_custodyFee             NUMERIC(18,2),
    expenses_operatingFee           NUMERIC(18,2),
    expenses_other                  NUMERIC(18,2),
    expenses_iof                    NUMERIC(18,2),
    expenses_iofProvision           NUMERIC(18,2)
);

-- =============================================================================
-- IDENTITY
-- =============================================================================
CREATE TABLE identity (
    id                      UUID            PRIMARY KEY,
    itemId                  UUID            NOT NULL REFERENCES items(id),
    fullName                VARCHAR(300),
    socialName              VARCHAR(300),
    companyName             VARCHAR(300),
    document                VARCHAR(30),
    documentType            VARCHAR(20),
    taxNumber               VARCHAR(30),
    jobTitle                VARCHAR(200),
    birthDate               TIMESTAMPTZ,
    incorporationDate       TIMESTAMPTZ,
    establishmentCode       VARCHAR(50),
    establishmentName       VARCHAR(200),
    investorProfile         VARCHAR(20),
    sex                     VARCHAR(10),
    maritalStatus_code              VARCHAR(30),
    maritalStatus_additionalInfo    VARCHAR(200),
    passport_number         VARCHAR(50),
    passport_country        VARCHAR(10),
    passport_issueDate      TIMESTAMPTZ,
    passport_expirationDate TIMESTAMPTZ,
    qual_companyCnpj                    VARCHAR(30),
    qual_occupationCode                 VARCHAR(30),
    qual_occupationDescription          VARCHAR(300),
    qual_informedIncome_frequency       VARCHAR(20),
    qual_informedIncome_amount          NUMERIC(18,2),
    qual_informedIncome_date            TIMESTAMPTZ,
    qual_informedPatrimony_amount       NUMERIC(18,2),
    qual_informedPatrimony_year         INTEGER,
    qual_informedPatrimony_date         TIMESTAMPTZ,
    qual_informedRevenue_amount         NUMERIC(18,2),
    qual_informedRevenue_frequency      VARCHAR(20),
    qual_informedRevenue_frequencyInfo  VARCHAR(200),
    qual_informedRevenue_year           INTEGER,
    finrel_startDate                            TIMESTAMPTZ,
    finrel_productsServicesTypeAdditionalInfo   VARCHAR(300),
    createdAt   TIMESTAMPTZ,
    updatedAt   TIMESTAMPTZ
);

CREATE TABLE identity_phone_numbers (
    id                  SERIAL          PRIMARY KEY,
    identity_id         UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    type                VARCHAR(20),
    value               VARCHAR(50)     NOT NULL,
    countryCallingCode  VARCHAR(10),
    areaCode            VARCHAR(10),
    extension           VARCHAR(20),
    additionalInfo      VARCHAR(200)
);

CREATE TABLE identity_emails (
    id          SERIAL          PRIMARY KEY,
    identity_id UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    type        VARCHAR(10),
    value       VARCHAR(200)    NOT NULL
);

CREATE TABLE identity_addresses (
    id              SERIAL          PRIMARY KEY,
    identity_id     UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    type            VARCHAR(10),
    fullAddress     TEXT,
    primaryAddress  VARCHAR(500),
    city            VARCHAR(200),
    postalCode      VARCHAR(20),
    state           VARCHAR(10),
    country         VARCHAR(100),
    countryCode     VARCHAR(10),
    additionalInfo  VARCHAR(500),
    district        VARCHAR(200),
    ibgeTownCode    VARCHAR(10),
    geo_latitude    NUMERIC(10,7),
    geo_longitude   NUMERIC(10,7)
);

CREATE TABLE identity_relations (
    id          SERIAL          PRIMARY KEY,
    identity_id UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    type        VARCHAR(20),
    name        VARCHAR(300),
    document    VARCHAR(30)
);

CREATE TABLE identity_other_documents (
    id                  SERIAL          PRIMARY KEY,
    identity_id         UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    type                VARCHAR(10),
    typeAdditionalInfo  VARCHAR(200),
    number              VARCHAR(50)     NOT NULL,
    checkDigit          VARCHAR(10),
    additionalInfo      VARCHAR(200),
    expirationDate      TIMESTAMPTZ
);

CREATE TABLE identity_other_nationalities (
    id          SERIAL      PRIMARY KEY,
    identity_id UUID        NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    countryCode VARCHAR(10) NOT NULL
);

CREATE TABLE identity_nationality_documents (
    id                      SERIAL          PRIMARY KEY,
    other_nationality_id    INTEGER         NOT NULL REFERENCES identity_other_nationalities(id) ON DELETE CASCADE,
    type                    VARCHAR(100)    NOT NULL,
    number                  VARCHAR(100)    NOT NULL,
    country                 VARCHAR(100),
    issueDate               TIMESTAMPTZ,
    expirationDate          TIMESTAMPTZ,
    additionalInfo          VARCHAR(300)
);

CREATE TABLE identity_parties (
    id                      SERIAL          PRIMARY KEY,
    identity_id             UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    type                    VARCHAR(20)     NOT NULL,
    personType              VARCHAR(20)     NOT NULL,
    documentType            VARCHAR(30)     NOT NULL,
    documentNumber          VARCHAR(50)     NOT NULL,
    documentCountry         VARCHAR(10),
    documentExpirationDate  TIMESTAMPTZ,
    documentIssueDate       TIMESTAMPTZ,
    documentAdditionalInfo  VARCHAR(300),
    civilName               VARCHAR(300),
    socialName              VARCHAR(300),
    companyName             VARCHAR(300),
    tradeName               VARCHAR(300),
    startDate               TIMESTAMPTZ,
    shareholding            NUMERIC(5,4)
);

CREATE TABLE identity_business_other_documents (
    id              SERIAL          PRIMARY KEY,
    identity_id     UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    type            VARCHAR(50)     NOT NULL,
    number          VARCHAR(100)    NOT NULL,
    country         VARCHAR(10)     NOT NULL,
    expirationDate  TIMESTAMPTZ
);

CREATE TABLE identity_economic_activities (
    id          SERIAL      PRIMARY KEY,
    identity_id UUID        NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    code        VARCHAR(10) NOT NULL,
    isMain      BOOLEAN     NOT NULL
);

CREATE TABLE identity_companies_cnpj (
    id          SERIAL      PRIMARY KEY,
    identity_id UUID        NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    cnpj        VARCHAR(20) NOT NULL
);

CREATE TABLE identity_finrel_products_services (
    id              SERIAL          PRIMARY KEY,
    identity_id     UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    product_service VARCHAR(200)    NOT NULL
);

CREATE TABLE identity_procurators (
    id              SERIAL          PRIMARY KEY,
    identity_id     UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    type            VARCHAR(30),
    cpfNumber       VARCHAR(20),
    documentNumber  VARCHAR(30),
    documentType    VARCHAR(10),
    civilName       VARCHAR(300),
    socialName      VARCHAR(300)
);

CREATE TABLE identity_portabilities_received (
    id                          SERIAL          PRIMARY KEY,
    identity_id                 UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    employerName                VARCHAR(300)    NOT NULL,
    employerDocument            VARCHAR(30)     NOT NULL,
    paycheckBankDetainerCnpj    VARCHAR(20)     NOT NULL,
    paycheckBankDetainerIspb    VARCHAR(20)     NOT NULL,
    portabilityApprovalDate     TIMESTAMPTZ     NOT NULL
);

CREATE TABLE identity_paycheck_bank_links (
    id                  SERIAL          PRIMARY KEY,
    identity_id         UUID            NOT NULL REFERENCES identity(id) ON DELETE CASCADE,
    employerName        VARCHAR(300)    NOT NULL,
    employerDocument    VARCHAR(30)     NOT NULL,
    paycheckBankCnpj    VARCHAR(20)     NOT NULL,
    paycheckBankIspb    VARCHAR(20)     NOT NULL,
    accountOpeningDate  TIMESTAMPTZ     NOT NULL
);

-- =============================================================================
-- BILLS
-- =============================================================================
CREATE TABLE bills (
    id                      UUID            PRIMARY KEY,
    accountId               UUID            NOT NULL REFERENCES accounts(id),
    dueDate                 TIMESTAMPTZ     NOT NULL,
    billClosingDate         TIMESTAMPTZ,
    totalAmount             NUMERIC(18,2)   NOT NULL,
    totalAmountCurrencyCode VARCHAR(10)     NOT NULL,
    minimumPaymentAmount    NUMERIC(18,2),
    allowsInstallments      BOOLEAN
);

CREATE TABLE bill_finance_charges (
    id              UUID            PRIMARY KEY,
    bill_id         UUID            NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    type            VARCHAR(60)     NOT NULL,
    amount          NUMERIC(18,2)   NOT NULL,
    currencyCode    VARCHAR(10)     NOT NULL,
    additionalInfo  VARCHAR(300)
);

CREATE TABLE bill_payments (
    id          UUID            PRIMARY KEY,
    bill_id     UUID            NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    valueType   VARCHAR(30)     NOT NULL,
    paymentDate TIMESTAMPTZ     NOT NULL,
    paymentMode VARCHAR(30),
    amount      NUMERIC(18,2)   NOT NULL,
    currencyCode VARCHAR(10)    NOT NULL
);

-- =============================================================================
-- ÍNDICES DE SUPORTE À INTEGRAÇÃO
-- =============================================================================
CREATE INDEX idx_items_connector_id        ON items(connector_id);
CREATE INDEX idx_accounts_item_id          ON accounts(itemId);
CREATE INDEX idx_transactions_account_id   ON transactions(accountId);
CREATE INDEX idx_transactions_date         ON transactions(date);
CREATE INDEX idx_transactions_category_id  ON transactions(categoryId);
CREATE INDEX idx_transactions_status       ON transactions(status);
CREATE INDEX idx_investments_item_id       ON investments(itemId);
CREATE INDEX idx_inv_transactions_inv_id   ON investment_transactions(investment_id);
CREATE INDEX idx_identity_item_id          ON identity(itemId);
CREATE INDEX idx_bills_account_id          ON bills(accountId);

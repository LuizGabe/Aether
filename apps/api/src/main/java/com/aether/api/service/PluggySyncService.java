package com.aether.api.service;

import com.aether.api.dto.SyncDtos.CreateConnectionRequest;
import com.aether.api.dto.SyncDtos.SyncByItemIdRequest;
import com.aether.api.dto.SyncDtos.SyncResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.UUID;
import java.util.HashSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PluggySyncService {

    private final PluggyClient pluggyClient;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ProductSyncService productSyncService;

    public PluggySyncService(PluggyClient pluggyClient, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, ProductSyncService productSyncService) {
        this.pluggyClient = pluggyClient;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.productSyncService = productSyncService;
    }

    @Transactional
    public String createConnection(CreateConnectionRequest request) {
        JsonNode item = pluggyClient.item(request.itemId());
        String existingName = jdbcTemplate.query("SELECT display_name FROM connections WHERE external_item_id=?",rs -> rs.next()?rs.getString(1):null,request.itemId());
        String displayName = firstText(request.displayName(), existingName, text(item, "connector", "name"), text(item, "name"), "Pluggy " + request.itemId());
        return upsertConnection(request.itemId(), displayName, item);
    }

    @Transactional(noRollbackFor = PluggyException.class)
    public SyncResult syncByItemId(SyncByItemIdRequest request) {
        String connectionId = findIdByExternal("connections", "external_item_id", request.itemId());
        if (connectionId == null) {
            connectionId = createConnection(new CreateConnectionRequest(request.itemId(), request.displayName()));
        } else if (StringUtils.hasText(request.displayName())) {
            jdbcTemplate.update("UPDATE connections SET display_name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", request.displayName().trim(), connectionId);
        }
        return sync(connectionId);
    }

    @Transactional(noRollbackFor = PluggyException.class)
    public SyncResult sync(String connectionId) {
        String itemId = jdbcTemplate.queryForObject(
            "SELECT external_item_id FROM connections WHERE id = ?",
            String.class,
            connectionId
        );
        if (!StringUtils.hasText(itemId)) {
            throw new PluggyException("CONNECTION_ITEM_MISSING", "Conexao sem itemId da Pluggy.");
        }

        String jobId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
            INSERT INTO sync_jobs (id, type, status, connection_id, attempts, started_at, created_at, updated_at)
            VALUES (?, 'pluggy_manual_sync', 'RUNNING', ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, jobId, connectionId);

        try {
            JsonNode item = waitForItemReady(itemId);
            refreshConnection(connectionId, item);
            if (!itemReady(item)) {
                String message = itemNotReadyMessage(item);
                completeJob(jobId, connectionId, item, 0, 0, 0, 0, "WAITING", "PLUGGY_ITEM_NOT_READY", message);
                return new SyncResult(connectionId, itemId, 0, 0, 0, 0, "WAITING", message);
            }

            int categories = syncCategories();
            AccountCounter accountCounter = syncAccounts(connectionId, itemId);
            int investments = syncInvestments(connectionId, itemId);
            var products=productSyncService.sync(connectionId);
            String status = accountCounter.accounts == 0 && investments == 0 ? "PARTIAL" : "SUCCESS";
            String code = "PARTIAL".equals(status) ? "PLUGGY_ITEM_WITHOUT_FINANCIAL_PRODUCTS" : null;
            String message = "PARTIAL".equals(status)
                ? noFinancialProductsMessage(item)
                : "Sincronizacao concluida.";
            if(!products.warnings().isEmpty()) {
                status="PARTIAL";code="OPTIONAL_PRODUCTS_UNAVAILABLE";
                message="Contas e transações importadas. "+String.join(" ",products.warnings());
            }
            completeJob(jobId, connectionId, item, accountCounter.accounts, accountCounter.transactions, categories, investments, status, code, message);
            jdbcTemplate.update("UPDATE sync_jobs SET metadata=json_set(metadata,'$.billsSynced',?,'$.investmentMovementsSynced',?) WHERE id=?",products.bills(),products.movements(),jobId);

            return new SyncResult(connectionId, itemId, accountCounter.accounts, accountCounter.transactions, categories, investments, status, message);
        } catch (RuntimeException error) {
            String code = error instanceof PluggyException pluggyException ? pluggyException.getCode() : "SYNC_FAILED";
            if ("ITEM_NOT_FOUND".equals(code)) {
                jdbcTemplate.update("UPDATE connections SET status = 'MISSING', execution_status = 'ITEM_NOT_FOUND' WHERE id = ?", connectionId);
            }
            jdbcTemplate.update("""
                UPDATE connections
                SET last_synced_at = CURRENT_TIMESTAMP, last_error_code = ?, last_error_message = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, code, error.getMessage(), connectionId);
            jdbcTemplate.update("""
                UPDATE sync_jobs
                SET status = 'FAILED', finished_at = CURRENT_TIMESTAMP, error_code = ?, error_message = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, code, error.getMessage(), jobId);
            throw error;
        }
    }

    private AccountCounter syncAccounts(String connectionId, String itemId) {
        int accountsSynced = 0;
        int transactionsSynced = 0;
        for (JsonNode account : results(pluggyClient.accounts(itemId))) {
            String accountId = upsertAccount(connectionId, account);
            accountsSynced++;
            transactionsSynced += syncTransactions(connectionId, accountId, requiredText(account, "id"));
        }
        return new AccountCounter(accountsSynced, transactionsSynced);
    }

    private int syncTransactions(String connectionId, String localAccountId, String externalAccountId) {
        int count = 0;
        String next = null;
        var visited = new HashSet<String>();
        for (int page = 1; page <= 100; page++) {
            JsonNode response = pluggyClient.transactionsPage(externalAccountId, next);
            for (JsonNode transaction : results(response)) {
                upsertTransaction(connectionId, localAccountId, transaction);
                count++;
            }
            next = text(response, "next");
            if (!StringUtils.hasText(next)) return count;
            if (!visited.add(next)) throw new PluggyException("PLUGGY_PAGINATION_INVALID", "A Pluggy repetiu uma pagina de transacoes. Tente sincronizar novamente.");
        }
        throw new PluggyException("PLUGGY_PAGINATION_LIMIT", "A sincronizacao excedeu o limite de paginas. Os dados importados foram preservados; a importacao esta incompleta.");
    }

    private int syncCategories() {
        int count = 0;
        for (JsonNode category : results(pluggyClient.categories())) {
            upsertCategory(category, null);
            count++;
            JsonNode children = category.get("children");
            if (children != null && children.isArray()) {
                for (JsonNode child : children) {
                    upsertCategory(child, requiredText(category, "id"));
                    count++;
                }
            }
        }
        return count;
    }

    private int syncInvestments(String connectionId, String itemId) {
        int count = 0;
        for (JsonNode investment : results(pluggyClient.investments(itemId))) {
            upsertInvestment(connectionId, investment);
            count++;
        }
        return count;
    }

    private String upsertConnection(String itemId, String displayName, JsonNode item) {
        String existingId = jdbcTemplate.query("""
            SELECT id FROM connections WHERE provider = 'pluggy' AND external_item_id = ? LIMIT 1
            """, rs -> rs.next() ? rs.getString("id") : null, itemId);
        String id = existingId == null ? UUID.randomUUID().toString() : existingId;
        jdbcTemplate.update("""
            INSERT INTO connections (
                id, provider, external_item_id, connector_id, display_name, status, execution_status, raw_payload, created_at, updated_at
            )
            VALUES (?, 'pluggy', ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(id) DO UPDATE SET
                external_item_id = excluded.external_item_id,
                connector_id = excluded.connector_id,
                display_name = excluded.display_name,
                status = excluded.status,
                execution_status = excluded.execution_status,
                raw_payload = excluded.raw_payload,
                updated_at = CURRENT_TIMESTAMP
            """, id, itemId, text(item, "connector", "id"), displayName, text(item, "status"), text(item, "executionStatus"), jsonString(item));
        return id;
    }

    private void refreshConnection(String connectionId, JsonNode item) {
        jdbcTemplate.update("""
            UPDATE connections
            SET connector_id = COALESCE(?, connector_id),
                display_name = COALESCE(NULLIF(display_name, ''), ?),
                status = COALESCE(?, status),
                execution_status = COALESCE(?, execution_status),
                raw_payload = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, text(item, "connector", "id"), text(item, "connector", "name"), text(item, "status"), text(item, "executionStatus"), jsonString(item), connectionId);
    }

    private String upsertAccount(String connectionId, JsonNode account) {
        String existingId = findIdByExternal("accounts", "external_account_id", requiredText(account, "id"));
        String id = existingId == null ? UUID.randomUUID().toString() : existingId;
        jdbcTemplate.update("""
            INSERT INTO accounts (
                id, connection_id, external_account_id, type, subtype, name, marketing_name, owner, number_masked,
                currency_code, balance, balance_date, credit_limit, available_credit_limit, raw_payload, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(id) DO UPDATE SET
                connection_id = excluded.connection_id,
                type = excluded.type,
                subtype = excluded.subtype,
                name = excluded.name,
                marketing_name = excluded.marketing_name,
                owner = excluded.owner,
                number_masked = excluded.number_masked,
                currency_code = excluded.currency_code,
                balance = excluded.balance,
                balance_date = excluded.balance_date,
                credit_limit = excluded.credit_limit,
                available_credit_limit = excluded.available_credit_limit,
                raw_payload = excluded.raw_payload,
                updated_at = CURRENT_TIMESTAMP
            """, id, connectionId, requiredText(account, "id"), text(account, "type"), text(account, "subtype"),
            text(account, "name"), text(account, "marketingName"), text(account, "owner"),
            firstText(text(account, "number"), text(account, "numberMasked")),
            firstText(text(account, "currencyCode"), "BRL"), decimal(account, "balance"), dateText(account, "balanceDate"),
            decimal(account.path("creditData"), "creditLimit"), decimal(account.path("creditData"), "availableCreditLimit"), jsonString(account));
        return id;
    }

    private void upsertTransaction(String connectionId, String accountId, JsonNode transaction) {
        String existingId = findIdByExternal("transactions", "external_transaction_id", requiredText(transaction, "id"));
        String id = existingId == null ? UUID.randomUUID().toString() : existingId;
        String pluggyCategory = firstText(text(transaction, "category"), text(transaction, "category", "description"));
        String categoryId = pluggyCategory == null ? null : ensureCategoryByName(pluggyCategory);
        jdbcTemplate.update("""
            INSERT INTO transactions (
                id, account_id, connection_id, external_transaction_id, date, description, description_raw, amount,
                amount_in_account_currency, currency_code, balance, type, status, provider_code, pluggy_category,
                category_id, categorization_source, raw_payload, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(id) DO UPDATE SET
                account_id = excluded.account_id,
                connection_id = excluded.connection_id,
                date = excluded.date,
                description = excluded.description,
                description_raw = excluded.description_raw,
                amount = excluded.amount,
                amount_in_account_currency = excluded.amount_in_account_currency,
                currency_code = excluded.currency_code,
                balance = excluded.balance,
                type = excluded.type,
                status = excluded.status,
                provider_code = excluded.provider_code,
                pluggy_category = excluded.pluggy_category,
                category_id = COALESCE(transactions.category_id, excluded.category_id),
                categorization_source = COALESCE(transactions.categorization_source, excluded.categorization_source),
                raw_payload = excluded.raw_payload,
                updated_at = CURRENT_TIMESTAMP
            """, id, accountId, connectionId, requiredText(transaction, "id"), dateText(transaction, "date"),
            firstText(text(transaction, "description"), text(transaction, "descriptionRaw")),
            text(transaction, "descriptionRaw"), decimal(transaction, "amount"), decimal(transaction, "amountInAccountCurrency"),
            firstText(text(transaction, "currencyCode"), "BRL"), decimal(transaction, "balance"), text(transaction, "type"),
            text(transaction, "status"), text(transaction, "providerCode"), pluggyCategory, categoryId,
            categoryId == null ? null : "pluggy", jsonString(transaction));
    }

    private void upsertCategory(JsonNode category, String parentId) {
        String id = requiredText(category, "id");
        jdbcTemplate.update("""
            INSERT INTO categories (id, parent_id, name, kind, color, icon, system, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(id) DO UPDATE SET
                parent_id = excluded.parent_id,
                name = excluded.name,
                kind = excluded.kind,
                color = excluded.color,
                icon = excluded.icon,
                system = 1,
                updated_at = CURRENT_TIMESTAMP
            """, id, parentId, firstText(text(category, "description"), text(category, "name"), id),
            firstText(text(category, "type"), "expense"), text(category, "color"), text(category, "icon"));
    }

    private String ensureCategoryByName(String name) {
        String id = jdbcTemplate.query("SELECT id FROM categories WHERE name = ? LIMIT 1", rs -> rs.next() ? rs.getString("id") : null, name);
        if (id != null) {
            return id;
        }
        id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
            INSERT INTO categories (id, name, kind, system, created_at, updated_at)
            VALUES (?, ?, 'expense', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, id, name);
        return id;
    }

    private void upsertInvestment(String connectionId, JsonNode investment) {
        String externalInvestmentId = requiredText(investment, "id");
        String existingId = findIdByExternal("investments", "external_investment_id", externalInvestmentId);
        String id = existingId == null ? UUID.randomUUID().toString() : existingId;
        String accountId = findIdByExternal("accounts", "external_account_id", text(investment, "accountId"));
        jdbcTemplate.update("""
            INSERT INTO investments (
                id, connection_id, account_id, external_investment_id, type, subtype, name, code, isin,
                quantity, value, amount, currency_code, date, raw_payload, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(id) DO UPDATE SET
                connection_id = excluded.connection_id,
                account_id = excluded.account_id,
                type = excluded.type,
                subtype = excluded.subtype,
                name = excluded.name,
                code = excluded.code,
                isin = excluded.isin,
                quantity = excluded.quantity,
                value = excluded.value,
                amount = excluded.amount,
                currency_code = excluded.currency_code,
                date = excluded.date,
                raw_payload = excluded.raw_payload,
                updated_at = CURRENT_TIMESTAMP
            """, id, connectionId, accountId, externalInvestmentId, text(investment, "type"), text(investment, "subtype"),
            firstText(text(investment, "name"), text(investment, "code"), externalInvestmentId), text(investment, "code"),
            text(investment, "isin"), decimal(investment, "quantity"), firstDecimal(investment, "value", "balance"),
            firstDecimal(investment, "amount", "grossAmount"), firstText(text(investment, "currencyCode"), "BRL"),
            firstText(dateText(investment, "date"), dateText(investment, "updatedAt")), jsonString(investment));
    }

    private JsonNode waitForItemReady(String itemId) {
        JsonNode item = pluggyClient.item(itemId);
        for (int attempt = 0; attempt < 20 && itemInProgress(item); attempt++) {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new PluggyException("SYNC_INTERRUPTED", "Sincronizacao interrompida enquanto aguardava a Pluggy.");
            }
            item = pluggyClient.item(itemId);
        }
        return item;
    }

    private Iterable<JsonNode> results(JsonNode node) {
        JsonNode results = node == null ? null : node.get("results");
        JsonNode iterable = results != null && results.isArray() ? results : node;
        return () -> iterable != null && iterable.isArray() ? iterable.elements() : new Iterator<>() {
            private boolean hasNext = iterable != null && iterable.isObject();

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public JsonNode next() {
                hasNext = false;
                return iterable;
            }
        };
    }

    private void completeJob(
        String jobId,
        String connectionId,
        JsonNode item,
        int accounts,
        int transactions,
        int categories,
        int investments,
        String status,
        String errorCode,
        String message
    ) {
        boolean success = "SUCCESS".equals(status) || "PARTIAL".equals(status);
        boolean complete = "SUCCESS".equals(status);
        jdbcTemplate.update("""
            UPDATE connections
            SET status = COALESCE(?, status), execution_status = COALESCE(?, execution_status),
                last_synced_at = CURRENT_TIMESTAMP,
                last_success_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE last_success_at END,
                last_error_code = ?, last_error_message = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, text(item, "status"), text(item, "executionStatus"), success, errorCode, complete ? null : message, connectionId);
        jdbcTemplate.update("""
            UPDATE sync_jobs
            SET status = ?, finished_at = CURRENT_TIMESTAMP, error_code = ?, error_message = ?,
                metadata = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, status, errorCode, complete ? null : message, jsonString(objectMapper.createObjectNode()
            .put("accountsSynced", accounts)
            .put("transactionsSynced", transactions)
            .put("categoriesSynced", categories)
            .put("investmentsSynced", investments)
            .put("itemStatus", firstText(text(item, "status"), "UNKNOWN"))
            .put("itemExecutionStatus", firstText(text(item, "executionStatus"), "UNKNOWN"))
            .set("itemStatusDetail", item == null ? null : item.get("statusDetail"))), jobId);
    }

    private boolean itemReady(JsonNode item) {
        String status = text(item, "status");
        return "UPDATED".equals(status) || "PARTIAL_SUCCESS".equals(status);
    }

    private boolean itemInProgress(JsonNode item) {
        String status = text(item, "status");
        return "UPDATING".equals(status) || "CREATED".equals(status);
    }

    private String itemNotReadyMessage(JsonNode item) {
        return "A Pluggy ainda nao liberou os produtos financeiros para este item. Status atual: "
            + firstText(text(item, "status"), "desconhecido")
            + ", executionStatus: "
            + firstText(text(item, "executionStatus"), "desconhecido")
            + ". Abra a conexao pelo widget se houver MFA/consentimento pendente e tente sincronizar novamente.";
    }

    private String noFinancialProductsMessage(JsonNode item) {
        return "A Pluggy retornou o item como pronto, mas nao retornou contas nem investimentos. "
            + "Categorias sao globais e podem sincronizar mesmo sem dados do item. StatusDetail: "
            + compactStatusDetail(item);
    }

    private String compactStatusDetail(JsonNode item) {
        JsonNode statusDetail = item == null ? null : item.get("statusDetail");
        return statusDetail == null || statusDetail.isNull() ? "indisponivel" : statusDetail.toString();
    }

    private String findIdByExternal(String table, String column, String externalId) {
        if (!StringUtils.hasText(externalId)) {
            return null;
        }
        return jdbcTemplate.query("SELECT id FROM " + table + " WHERE " + column + " = ? LIMIT 1", rs -> rs.next() ? rs.getString("id") : null, externalId);
    }

    private String jsonString(JsonNode node) {
        if (node == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException error) {
            return node.toString();
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            throw new PluggyException("PLUGGY_PAYLOAD_INVALID", "Resposta Pluggy sem campo obrigatorio: " + field);
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private static String text(JsonNode node, String parent, String field) {
        return node == null ? null : text(node.get(parent), field);
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return new BigDecimal(value.asText());
    }

    private static BigDecimal firstDecimal(JsonNode node, String... fields) {
        for (String field : fields) {
            BigDecimal value = decimal(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String dateText(JsonNode node, String field) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.substring(0, Math.min(10, value.length()))).toString();
    }

    private record AccountCounter(int accounts, int transactions) {}
}

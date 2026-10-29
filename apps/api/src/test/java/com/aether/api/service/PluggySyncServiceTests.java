package com.aether.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.aether.api.dto.SyncDtos.SyncByItemIdRequest;
import com.aether.api.dto.SyncDtos.SyncResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PluggySyncServiceTests {

    @Autowired
    private PluggySyncService pluggySyncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FinanceQueryService financeQueryService;

    @MockitoBean
    private PluggyClient pluggyClient;

    @BeforeEach
    void clearData() {
        for (String table : new String[] {"classification_rules","card_bills","investment_movements","investment_snapshots","sync_jobs", "investments", "transactions", "accounts", "connections", "institutions", "categories"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void missingRemoteItemKeepsFailureHistoryAndLocalData() {
        jdbcTemplate.update("INSERT INTO connections (id, external_item_id, display_name, last_success_at) VALUES ('local', 'gone', 'Banco', CURRENT_TIMESTAMP)");
        when(pluggyClient.item("gone")).thenThrow(new PluggyException("ITEM_NOT_FOUND", "Conexao nao encontrada."));

        assertThatThrownBy(() -> pluggySyncService.syncByItemId(new SyncByItemIdRequest("gone", null)))
            .isInstanceOf(PluggyException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM connections WHERE id = 'local'", String.class)).isEqualTo("MISSING");
        assertThat(jdbcTemplate.queryForObject("SELECT last_error_code FROM connections WHERE id = 'local'", String.class)).isEqualTo("ITEM_NOT_FOUND");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM sync_jobs WHERE connection_id = 'local'", String.class)).isEqualTo("FAILED");
        assertCount("connections", 1);
        assertThat(financeQueryService.summary().get("connections")).isZero();
    }

    @Test
    void syncByItemIdUpsertsPluggyDataWithoutDuplicates() throws Exception {
        when(pluggyClient.item("item-1")).thenReturn(json("""
            {
              "id": "item-1",
              "status": "UPDATED",
              "executionStatus": "SUCCESS",
              "connector": { "id": 1, "name": "Pluggy Bank" }
            }
            """));
        when(pluggyClient.categories()).thenReturn(json("""
            {
              "results": [
                { "id": "cat-food", "description": "Alimentacao", "type": "expense" }
              ]
            }
            """));
        when(pluggyClient.accounts("item-1")).thenReturn(json("""
            {
              "results": [
                {
                  "id": "account-1",
                  "type": "BANK",
                  "subtype": "CHECKING_ACCOUNT",
                  "name": "Conta Corrente",
                  "currencyCode": "BRL",
                  "balance": 123.45,
                  "balanceDate": "2026-09-13"
                }
              ]
            }
            """));
        when(pluggyClient.transactionsPage(eq("account-1"), isNull())).thenReturn(json("""
            {
              "pageSize": 500,
              "next": null,
              "results": [
                {
                  "id": "transaction-1",
                  "date": "2026-09-12",
                  "description": "Mercado",
                  "amount": -50.10,
                  "currencyCode": "BRL",
                  "category": "Alimentacao",
                  "status": "POSTED"
                }
              ]
            }
            """));
        when(pluggyClient.investments("item-1")).thenReturn(json("""
            {
              "results": [
                {
                  "id": "investment-1",
                  "accountId": "account-1",
                  "type": "SECURITY",
                  "name": "Tesouro Selic",
                  "quantity": 2,
                  "value": 2000.00,
                  "currencyCode": "BRL",
                  "date": "2026-09-13"
                }
              ]
            }
            """));

        SyncResult first = pluggySyncService.syncByItemId(new SyncByItemIdRequest("item-1", "Banco pessoal"));
        jdbcTemplate.update("UPDATE transactions SET counterparty_override='Nome revisado' WHERE external_transaction_id='transaction-1'");
        SyncResult second = pluggySyncService.syncByItemId(new SyncByItemIdRequest("item-1", null));
        assertThat(jdbcTemplate.queryForObject("SELECT counterparty_override FROM transactions WHERE external_transaction_id='transaction-1'",String.class)).isEqualTo("Nome revisado");

        assertThat(first.accountsSynced()).isEqualTo(1);
        assertThat(first.transactionsSynced()).isEqualTo(1);
        assertThat(first.categoriesSynced()).isEqualTo(1);
        assertThat(first.investmentsSynced()).isEqualTo(1);
        assertThat(second.status()).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject("SELECT display_name FROM connections WHERE external_item_id = 'item-1'", String.class)).isEqualTo("Banco pessoal");
        assertCount("connections", 1);
        assertCount("accounts", 1);
        assertCount("transactions", 1);
        assertCount("categories", 1);
        assertCount("investments", 1);
        assertCount("sync_jobs", 2);
        // The panel must count persisted rows rather than the capped transaction list.
        assertThat(financeQueryService.summary()).containsEntry("transactions", 1).containsEntry("accounts", 1);
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }

    private void assertCount(String table, int expected) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        assertThat(count).isEqualTo(expected);
    }
}

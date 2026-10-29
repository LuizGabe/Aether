package com.aether.api.service;

import com.aether.api.dto.AccountDto;
import com.aether.api.dto.CategoryDto;
import com.aether.api.dto.ConnectionDto;
import com.aether.api.dto.InvestmentDto;
import com.aether.api.dto.SyncJobDto;
import com.aether.api.dto.TransactionDto;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class FinanceQueryService {

    private final JdbcTemplate jdbcTemplate;

    public FinanceQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Integer> summary() {
        return jdbcTemplate.queryForObject("""
            SELECT (SELECT COUNT(*) FROM connections WHERE status IS NULL OR status <> 'MISSING') AS connections,
                   (SELECT COUNT(*) FROM accounts) AS accounts,
                   (SELECT COUNT(*) FROM transactions) AS transactions,
                   (SELECT COUNT(*) FROM categories) AS categories,
                   (SELECT COUNT(*) FROM investments) AS investments
            """, (rs, rowNum) -> Map.of("connections", rs.getInt("connections"), "accounts", rs.getInt("accounts"),
                "transactions", rs.getInt("transactions"), "categories", rs.getInt("categories"), "investments", rs.getInt("investments")));
    }

    public List<ConnectionDto> connections() {
        return jdbcTemplate.query("""
            SELECT id, provider, external_item_id, connector_id, display_name, status, execution_status,
                   last_synced_at, last_success_at, last_error_code, last_error_message
            FROM connections
            ORDER BY updated_at DESC
            """, (rs, rowNum) -> new ConnectionDto(
            rs.getString("id"),
            rs.getString("provider"),
            rs.getString("external_item_id"),
            rs.getString("connector_id"),
            rs.getString("display_name"),
            rs.getString("status"),
            rs.getString("execution_status"),
            offsetDateTime(rs, "last_synced_at"),
            offsetDateTime(rs, "last_success_at"),
            rs.getString("last_error_code"),
            rs.getString("last_error_message")
        ));
    }

    public List<AccountDto> accounts() {
        return jdbcTemplate.query("""
            SELECT id, connection_id, external_account_id, type, subtype, name, marketing_name, owner,
                   number_masked, currency_code, balance, balance_date, credit_limit,
                   available_credit_limit, hidden, updated_at
            FROM accounts
            ORDER BY name
            """, (rs, rowNum) -> new AccountDto(
            rs.getString("id"),
            rs.getString("connection_id"),
            rs.getString("external_account_id"),
            rs.getString("type"),
            rs.getString("subtype"),
            rs.getString("name"),
            rs.getString("marketing_name"),
            rs.getString("owner"),
            rs.getString("number_masked"),
            rs.getString("currency_code"),
            bigDecimal(rs, "balance"),
            localDate(rs, "balance_date"),
            bigDecimal(rs, "credit_limit"),
            bigDecimal(rs, "available_credit_limit"),
            rs.getBoolean("hidden"),
            offsetDateTime(rs, "updated_at")
        ));
    }

    public List<TransactionDto> transactions() {
        return jdbcTemplate.query("""
            SELECT id, account_id, connection_id, external_transaction_id, date, description, description_raw,
                   amount, amount_in_account_currency, currency_code, balance, type, status, provider_code,
                   pluggy_category, category_id, categorization_source, ignored, updated_at
            FROM transactions
            ORDER BY date DESC, updated_at DESC
            LIMIT 500
            """, (rs, rowNum) -> new TransactionDto(
            rs.getString("id"),
            rs.getString("account_id"),
            rs.getString("connection_id"),
            rs.getString("external_transaction_id"),
            localDate(rs, "date"),
            rs.getString("description"),
            rs.getString("description_raw"),
            bigDecimal(rs, "amount"),
            bigDecimal(rs, "amount_in_account_currency"),
            rs.getString("currency_code"),
            bigDecimal(rs, "balance"),
            rs.getString("type"),
            rs.getString("status"),
            rs.getString("provider_code"),
            rs.getString("pluggy_category"),
            rs.getString("category_id"),
            rs.getString("categorization_source"),
            rs.getBoolean("ignored"),
            offsetDateTime(rs, "updated_at")
        ));
    }

    public List<CategoryDto> categories() {
        return jdbcTemplate.query("""
            SELECT id, parent_id, name, kind, color, icon, system
            FROM categories
            ORDER BY kind, name
            """, (rs, rowNum) -> new CategoryDto(
            rs.getString("id"),
            rs.getString("parent_id"),
            rs.getString("name"),
            rs.getString("kind"),
            rs.getString("color"),
            rs.getString("icon"),
            rs.getBoolean("system")
        ));
    }

    public List<InvestmentDto> investments() {
        return jdbcTemplate.query("""
            SELECT id, connection_id, account_id, external_investment_id, type, subtype, name, code, isin,
                   quantity, value, amount, currency_code, date
            FROM investments
            ORDER BY name
            """, (rs, rowNum) -> new InvestmentDto(
            rs.getString("id"),
            rs.getString("connection_id"),
            rs.getString("account_id"),
            rs.getString("external_investment_id"),
            rs.getString("type"),
            rs.getString("subtype"),
            rs.getString("name"),
            rs.getString("code"),
            rs.getString("isin"),
            bigDecimal(rs, "quantity"),
            bigDecimal(rs, "value"),
            bigDecimal(rs, "amount"),
            rs.getString("currency_code"),
            localDate(rs, "date")
        ));
    }

    public List<SyncJobDto> syncJobs() {
        return jdbcTemplate.query("""
            SELECT id, type, status, connection_id, attempts, started_at, finished_at,
                   error_code, error_message, metadata, created_at
            FROM sync_jobs
            ORDER BY created_at DESC
            LIMIT 50
            """, (rs, rowNum) -> new SyncJobDto(
            rs.getString("id"),
            rs.getString("type"),
            rs.getString("status"),
            rs.getString("connection_id"),
            rs.getInt("attempts"),
            offsetDateTime(rs, "started_at"),
            offsetDateTime(rs, "finished_at"),
            rs.getString("error_code"),
            rs.getString("error_message"),
            rs.getString("metadata"),
            offsetDateTime(rs, "created_at")
        ));
    }

    private static BigDecimal bigDecimal(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : new BigDecimal(value);
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.substring(0, Math.min(10, value.length())));
    }

    private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.replace(" ", "T") + (value.endsWith("Z") ? "" : "Z"));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}

package com.aether.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    private LocalDate date;
    private String description;

    @Column(name = "description_raw")
    private String descriptionRaw;

    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "amount_in_account_currency")
    private BigDecimal amountInAccountCurrency;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode = "BRL";

    private BigDecimal balance;
    private String type;
    private String status;

    @Column(name = "provider_code")
    private String providerCode;

    @Column(name = "pluggy_category")
    private String pluggyCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "categorization_source")
    private String categorizationSource;

    private String notes;
    private boolean ignored;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public String getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Institution getInstitution() {
        return institution;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getDescriptionRaw() {
        return descriptionRaw;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getAmountInAccountCurrency() {
        return amountInAccountCurrency;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getPluggyCategory() {
        return pluggyCategory;
    }

    public Category getCategory() {
        return category;
    }

    public String getCategorizationSource() {
        return categorizationSource;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

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
@Table(name = "accounts")
public class Account {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(name = "external_account_id")
    private String externalAccountId;

    private String type;
    private String subtype;
    private String name;

    @Column(name = "marketing_name")
    private String marketingName;

    private String owner;

    @Column(name = "number_masked")
    private String numberMasked;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode = "BRL";

    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "balance_date")
    private LocalDate balanceDate;

    @Column(name = "credit_limit")
    private BigDecimal creditLimit;

    @Column(name = "available_credit_limit")
    private BigDecimal availableCreditLimit;

    private boolean hidden;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public String getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getName() {
        return name;
    }

    public String getMarketingName() {
        return marketingName;
    }

    public String getOwner() {
        return owner;
    }

    public String getNumberMasked() {
        return numberMasked;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDate getBalanceDate() {
        return balanceDate;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getAvailableCreditLimit() {
        return availableCreditLimit;
    }

    public boolean isHidden() {
        return hidden;
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

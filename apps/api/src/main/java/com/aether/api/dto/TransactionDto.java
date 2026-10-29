package com.aether.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TransactionDto(
    String id,
    String accountId,
    String connectionId,
    String externalTransactionId,
    LocalDate date,
    String description,
    String descriptionRaw,
    BigDecimal amount,
    BigDecimal amountInAccountCurrency,
    String currencyCode,
    BigDecimal balance,
    String type,
    String status,
    String providerCode,
    String pluggyCategory,
    String categoryId,
    String categorizationSource,
    boolean ignored,
    OffsetDateTime updatedAt
) {}

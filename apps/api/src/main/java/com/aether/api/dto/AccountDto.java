package com.aether.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AccountDto(
    String id,
    String connectionId,
    String externalAccountId,
    String type,
    String subtype,
    String name,
    String marketingName,
    String owner,
    String numberMasked,
    String currencyCode,
    BigDecimal balance,
    LocalDate balanceDate,
    BigDecimal creditLimit,
    BigDecimal availableCreditLimit,
    boolean hidden,
    OffsetDateTime updatedAt
) {}

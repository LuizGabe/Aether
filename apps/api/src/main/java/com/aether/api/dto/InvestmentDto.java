package com.aether.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentDto(
    String id,
    String connectionId,
    String accountId,
    String externalInvestmentId,
    String type,
    String subtype,
    String name,
    String code,
    String isin,
    BigDecimal quantity,
    BigDecimal value,
    BigDecimal amount,
    String currencyCode,
    LocalDate date
) {}

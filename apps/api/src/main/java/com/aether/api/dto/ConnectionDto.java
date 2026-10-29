package com.aether.api.dto;

import java.time.OffsetDateTime;

public record ConnectionDto(
    String id,
    String provider,
    String externalItemId,
    String connectorId,
    String displayName,
    String status,
    String executionStatus,
    OffsetDateTime lastSyncedAt,
    OffsetDateTime lastSuccessAt,
    String lastErrorCode,
    String lastErrorMessage
) {}

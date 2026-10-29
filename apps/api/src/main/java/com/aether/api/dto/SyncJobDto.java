package com.aether.api.dto;

import java.time.OffsetDateTime;

public record SyncJobDto(
    String id,
    String type,
    String status,
    String connectionId,
    int attempts,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    String errorCode,
    String errorMessage,
    String metadata,
    OffsetDateTime createdAt
) {}

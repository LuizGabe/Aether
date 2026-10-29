package com.aether.api.dto;

import jakarta.validation.constraints.NotBlank;

public final class SyncDtos {
    private SyncDtos() {}

    public record PluggyCredentialsRequest(
        @NotBlank String clientId,
        @NotBlank String clientSecret
    ) {}

    public record ConnectTokenRequest(
        String itemId
    ) {}

    public record ConnectTokenResponse(
        String accessToken
    ) {}

    public record CreateConnectionRequest(
        @NotBlank String itemId,
        String displayName
    ) {}

    public record SyncByItemIdRequest(
        @NotBlank String itemId,
        String displayName
    ) {}

    public record SyncResult(
        String connectionId,
        String itemId,
        int accountsSynced,
        int transactionsSynced,
        int categoriesSynced,
        int investmentsSynced,
        String status,
        String message
    ) {}
}

package com.aether.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "institutions")
public class Institution {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String provider = "pluggy";

    @Column(name = "external_item_id")
    private String externalItemId;

    @Column(name = "connector_id")
    private String connectorId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String status;

    @Column(name = "execution_status")
    private String executionStatus;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public String getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getExternalItemId() {
        return externalItemId;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStatus() {
        return status;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public OffsetDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

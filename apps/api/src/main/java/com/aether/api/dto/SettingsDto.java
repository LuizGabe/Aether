package com.aether.api.dto;

public record SettingsDto(
    boolean pluggyCredentialsConfigured,
    String pluggyCredentialsSource
) {}

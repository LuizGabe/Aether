package com.aether.api.service;

import com.aether.api.dto.SettingsDto;
import com.aether.api.dto.SyncDtos.PluggyCredentialsRequest;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

@Service
public class PluggyCredentialsService {

    private final AtomicReference<PluggyCredentials> runtimeCredentials = new AtomicReference<>();

    private final JdbcTemplate jdbc;
    public PluggyCredentialsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private boolean disabled() { return !jdbc.queryForList("SELECT key FROM app_settings WHERE key='pluggy_credentials_disabled' AND value='true'").isEmpty(); }
    public void forgetRuntime() { runtimeCredentials.set(null); }

    public SettingsDto settings() {
        if (disabled()) return new SettingsDto(false, "none");
        if (runtimeCredentials.get() != null) {
            return new SettingsDto(true, "runtime");
        }

        if (hasEnvCredentials()) {
            return new SettingsDto(true, "environment");
        }

        return new SettingsDto(false, "none");
    }

    public PluggyCredentials resolve() {
        if (disabled()) throw new PluggyException("PLUGGY_CREDENTIALS_MISSING", "Configure as credenciais da integração para conectar seus bancos.");
        PluggyCredentials runtime = runtimeCredentials.get();
        if (runtime != null) {
            return runtime;
        }

        String clientId = System.getenv("AETHER_PLUGGY_CLIENT_ID");
        String clientSecret = System.getenv("AETHER_PLUGGY_CLIENT_SECRET");

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            clientId = System.getenv("PLUGGY_CLIENT_ID");
            clientSecret = System.getenv("PLUGGY_CLIENT_SECRET");
        }

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new PluggyException("PLUGGY_CREDENTIALS_MISSING", "Credenciais Pluggy nao configuradas.");
        }

        return new PluggyCredentials(clientId.trim(), clientSecret.trim());
    }

    public SettingsDto update(PluggyCredentialsRequest request) {
        jdbc.update("DELETE FROM app_settings WHERE key='pluggy_credentials_disabled'");
        runtimeCredentials.set(new PluggyCredentials(request.clientId().trim(), request.clientSecret().trim()));
        return settings();
    }

    public SettingsDto clearRuntime() {
        runtimeCredentials.set(null);
        return settings();
    }

    private boolean hasEnvCredentials() {
        return (StringUtils.hasText(System.getenv("AETHER_PLUGGY_CLIENT_ID"))
            && StringUtils.hasText(System.getenv("AETHER_PLUGGY_CLIENT_SECRET")))
            || (StringUtils.hasText(System.getenv("PLUGGY_CLIENT_ID"))
            && StringUtils.hasText(System.getenv("PLUGGY_CLIENT_SECRET")));
    }

    public record PluggyCredentials(String clientId, String clientSecret) {}
}

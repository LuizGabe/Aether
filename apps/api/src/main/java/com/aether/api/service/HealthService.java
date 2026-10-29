package com.aether.api.service;

import com.aether.api.dto.HealthResponse;
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final DataSource dataSource;
    private final String version;

    public HealthService(DataSource dataSource, ObjectProvider<BuildProperties> buildProperties) {
        this.dataSource = dataSource;
        BuildProperties properties = buildProperties.getIfAvailable();
        this.version = properties == null ? "0.2.0" : properties.getVersion();
    }

    public HealthResponse current() {
        return new HealthResponse("ok", version, databaseStatus());
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "ok" : "unavailable";
        } catch (Exception exception) {
            return "unavailable";
        }
    }
}

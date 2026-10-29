package com.aether.api.controller;

import com.aether.api.dto.SettingsDto;
import com.aether.api.dto.SyncDtos.PluggyCredentialsRequest;
import com.aether.api.service.PluggyCredentialsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final PluggyCredentialsService credentialsService;

    public SettingsController(PluggyCredentialsService credentialsService) {
        this.credentialsService = credentialsService;
    }

    @GetMapping
    public SettingsDto get() {
        return credentialsService.settings();
    }

    @PutMapping("/pluggy-credentials")
    public SettingsDto updatePluggyCredentials(@Valid @RequestBody PluggyCredentialsRequest request) {
        return credentialsService.update(request);
    }

    @DeleteMapping("/pluggy-credentials")
    public SettingsDto deletePluggyCredentials() {
        return credentialsService.clearRuntime();
    }
}

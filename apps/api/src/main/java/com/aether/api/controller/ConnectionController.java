package com.aether.api.controller;

import com.aether.api.dto.ConnectionDto;
import com.aether.api.dto.SyncDtos.CreateConnectionRequest;
import com.aether.api.dto.SyncDtos.SyncByItemIdRequest;
import com.aether.api.dto.SyncDtos.SyncResult;
import com.aether.api.service.FinanceQueryService;
import com.aether.api.service.PluggySyncService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final FinanceQueryService financeQueryService;
    private final PluggySyncService pluggySyncService;

    public ConnectionController(FinanceQueryService financeQueryService, PluggySyncService pluggySyncService) {
        this.financeQueryService = financeQueryService;
        this.pluggySyncService = pluggySyncService;
    }

    @GetMapping
    public List<ConnectionDto> list() {
        return financeQueryService.connections();
    }

    @PostMapping
    public ConnectionDto create(@Valid @RequestBody CreateConnectionRequest request) {
        String id = pluggySyncService.createConnection(request);
        return financeQueryService.connections().stream()
            .filter(connection -> connection.id().equals(id))
            .findFirst()
            .orElseThrow();
    }

    @PostMapping("/{id}/sync")
    public SyncResult sync(@PathVariable String id) {
        return pluggySyncService.sync(id);
    }

    @PostMapping("/sync-by-item-id")
    public SyncResult syncByItemId(@Valid @RequestBody SyncByItemIdRequest request) {
        return pluggySyncService.syncByItemId(request);
    }
}

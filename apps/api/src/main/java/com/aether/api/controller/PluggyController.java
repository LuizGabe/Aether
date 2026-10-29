package com.aether.api.controller;

import com.aether.api.dto.SyncDtos.ConnectTokenRequest;
import com.aether.api.dto.SyncDtos.ConnectTokenResponse;
import com.aether.api.service.PluggyClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pluggy")
public class PluggyController {

    private final PluggyClient pluggyClient;

    public PluggyController(PluggyClient pluggyClient) {
        this.pluggyClient = pluggyClient;
    }

    @PostMapping("/connect-tokens")
    public ConnectTokenResponse createConnectToken(@RequestBody(required = false) ConnectTokenRequest request) {
        return pluggyClient.createConnectToken(request == null ? null : request.itemId());
    }

    @GetMapping("/connectors")
    public JsonNode connectors() {
        return pluggyClient.connectors();
    }

    @GetMapping("/items")
    public JsonNode items() {
        return pluggyClient.items();
    }

    @GetMapping("/items/{itemId}")
    public JsonNode item(@PathVariable String itemId) {
        return pluggyClient.item(itemId);
    }

    @GetMapping("/accounts")
    public JsonNode accounts(@RequestParam String itemId, @RequestParam(required = false) String type) {
        return type == null || type.isBlank() ? pluggyClient.accounts(itemId) : pluggyClient.accountsByType(itemId, type);
    }

    @GetMapping("/transactions")
    public JsonNode transactions(@RequestParam String accountId, @RequestParam(defaultValue = "1") int page) {
        return pluggyClient.transactions(accountId, page);
    }

    @GetMapping("/v2/transactions")
    public JsonNode transactionsV2(@RequestParam String accountId, @RequestParam(required = false) String after) {
        return pluggyClient.transactionsV2(accountId, after);
    }

    @GetMapping("/investments")
    public JsonNode investments(@RequestParam String itemId) {
        return pluggyClient.investments(itemId);
    }
}

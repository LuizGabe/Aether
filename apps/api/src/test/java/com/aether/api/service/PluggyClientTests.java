package com.aether.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PluggyClientTests {
    private final PluggyCredentialsService credentials = mock(PluggyCredentialsService.class);
    private MockRestServiceServer server;
    private PluggyClient client;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // Test requests must use the mock factory rather than a real network factory.
        client = new PluggyClient(credentials, builder, false);
        when(credentials.resolve()).thenReturn(new PluggyCredentialsService.PluggyCredentials("client", "secret"));
    }

    private void auth(String key) {
        server.expect(requestTo("https://api.pluggy.ai/auth"))
            .andRespond(withSuccess("{\"apiKey\":\"" + key + "\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    void listsAllPagesUsingOfficialHostAndApiKey() {
        auth("key");
        server.expect(requestTo("https://api.pluggy.ai/v2/items"))
            .andExpect(header("X-API-KEY", "key"))
            .andRespond(withSuccess("{\"results\":[{\"id\":\"one\"}],\"next\":\"?after=abc%2B123&connectorId=200\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/v2/items?after=abc%2B123&connectorId=200"))
            .andExpect(header("X-API-KEY", "key"))
            .andRespond(withSuccess("{\"results\":[{\"id\":\"two\"}],\"next\":null}", MediaType.APPLICATION_JSON));
        assertThat(client.items().path("results")).hasSize(2);
        server.verify();
    }

    @Test
    void explainsListingRestrictionWithoutExposingUpstreamBody() {
        auth("key");
        server.expect(requestTo("https://api.pluggy.ai/v2/items"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                .body("{\"codeDescription\":\"LIST_ITEMS_FEATURE_NOT_ENABLED\",\"message\":\"sensitive upstream detail\"}"));
        assertThatThrownBy(client::items).isInstanceOfSatisfying(PluggyException.class, error -> {
            assertThat(error.getCode()).isEqualTo("LIST_ITEMS_FEATURE_NOT_ENABLED");
            assertThat(error.getMessage()).contains("Conectar com Pluggy").doesNotContain("sensitive");
        });
        server.verify();
    }

    @Test
    void rejectsExternalPaginationUrls() {
        auth("key");
        server.expect(requestTo("https://api.pluggy.ai/v2/items"))
            .andRespond(withSuccess("{\"results\":[],\"next\":\"https://example.com/steal\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(client::items).isInstanceOfSatisfying(PluggyException.class,
            error -> assertThat(error.getCode()).isEqualTo("PLUGGY_PAGINATION_INVALID"));
        server.verify();
    }

    @Test
    void transactionsDoNotSendUnsupportedPageSizeAndPreserveNextQuery() {
        auth("key");
        server.expect(requestTo("https://api.pluggy.ai/v2/transactions?accountId=account-1"))
            .andRespond(withSuccess("{\"results\":[],\"next\":\"?accountId=account-1&after=a%2Bb%3D\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/v2/transactions?accountId=account-1&after=a%2Bb%3D"))
            .andRespond(withSuccess("{\"results\":[],\"next\":null}", MediaType.APPLICATION_JSON));
        var page = client.transactionsPage("account-1", null);
        client.transactionsPage("account-1", page.path("next").asText());
        server.verify();
    }

    @Test
    void renewsRejectedApiKeyOnce() {
        auth("expired-key");
        server.expect(requestTo("https://api.pluggy.ai/items/one"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"expired\"}"));
        auth("fresh-key");
        server.expect(requestTo("https://api.pluggy.ai/items/one"))
            .andExpect(header("X-API-KEY", "fresh-key"))
            .andRespond(withSuccess("{\"id\":\"one\"}", MediaType.APPLICATION_JSON));
        assertThat(client.item("one").path("id").asText()).isEqualTo("one");
        server.verify();
    }

    @Test
    void changingCredentialsDoesNotReusePreviousApiKey() {
        auth("key-one");
        auth("key-two");
        assertThat(client.apiKey()).isEqualTo("key-one");
        when(credentials.resolve()).thenReturn(new PluggyCredentialsService.PluggyCredentials("other", "secret-two"));
        assertThat(client.apiKey()).isEqualTo("key-two");
        server.verify();
    }
}

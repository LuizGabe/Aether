package com.aether.api.service;

import com.aether.api.dto.SyncDtos.ConnectTokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.HashSet;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

@Component
public class PluggyClient {

    private static final String BASE_URL = "https://api.pluggy.ai";

    private final PluggyCredentialsService credentialsService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private PluggyCredentialsService.PluggyCredentials cachedCredentials;
    private String cachedApiKey;
    private Instant cachedApiKeyExpiresAt = Instant.EPOCH;

    @Autowired
    public PluggyClient(PluggyCredentialsService credentialsService, RestClient.Builder restClientBuilder) {
        this(credentialsService, restClientBuilder, true);
    }

    PluggyClient(PluggyCredentialsService credentialsService, RestClient.Builder restClientBuilder, boolean configureTimeouts) {
        this.credentialsService = credentialsService;
        var builder = restClientBuilder.clone();
        if (configureTimeouts) builder.requestFactory(requestFactory());
        this.restClient = builder
            .baseUrl(BASE_URL)
            .build();
    }

    public synchronized String apiKey() {
        PluggyCredentialsService.PluggyCredentials credentials = credentialsService.resolve();
        if (credentials.equals(cachedCredentials) && cachedApiKey != null && Instant.now().isBefore(cachedApiKeyExpiresAt.minusSeconds(60))) {
            return cachedApiKey;
        }

        JsonNode response = restClient.post()
            .uri("/auth")
            .body(Map.of("clientId", credentials.clientId(), "clientSecret", credentials.clientSecret()))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                throw pluggyError("PLUGGY_AUTH_FAILED", "A Pluggy recusou as credenciais configuradas.", clientResponse);
            })
            .body(JsonNode.class);

        JsonNode apiKey = response == null ? null : response.get("apiKey");
        if (apiKey == null || apiKey.asText().isBlank()) {
            throw new PluggyException("PLUGGY_AUTH_EMPTY", "A Pluggy nao retornou uma API key.");
        }

        cachedApiKey = apiKey.asText();
        cachedCredentials = credentials;
        cachedApiKeyExpiresAt = Instant.now().plusSeconds(2 * 60 * 60);
        return cachedApiKey;
    }

    public ConnectTokenResponse createConnectToken(String itemId) {
        Map<String, Object> body = itemId == null || itemId.isBlank()
            ? Map.of("options", Map.of("clientUserId", "aether-local", "avoidDuplicates", true))
            : Map.of("itemId", itemId);

        JsonNode response = restClient.post()
            .uri("/connect_token")
            .header("X-API-KEY", apiKey())
            .body(body)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                throw pluggyError("PLUGGY_CONNECT_TOKEN_FAILED", "Nao foi possivel criar o token do Pluggy Connect.", clientResponse);
            })
            .body(JsonNode.class);

        JsonNode accessToken = response == null ? null : response.get("accessToken");
        if (accessToken == null || accessToken.asText().isBlank()) {
            throw new PluggyException("PLUGGY_CONNECT_TOKEN_EMPTY", "A Pluggy nao retornou um connect token.");
        }

        return new ConnectTokenResponse(accessToken.asText());
    }

    public JsonNode connectors() {
        return get("/connectors");
    }

    public JsonNode item(String itemId) {
        return get("/items/" + itemId);
    }

    public JsonNode items() {
        var items = objectMapper.createArrayNode();
        var visited = new HashSet<String>();
        String next = "";
        do {
            // next is a query string, never a URL. Keep credentials on the official host.
            if ((!next.isEmpty() && !next.startsWith("?")) || !visited.add(next) || visited.size() > 100) {
                throw new PluggyException("PLUGGY_PAGINATION_INVALID", "A Pluggy retornou uma paginacao invalida. Tente novamente mais tarde.");
            }
            JsonNode page = get("/v2/items" + next);
            if (page == null || !page.path("results").isArray()) {
                throw new PluggyException("PLUGGY_PAYLOAD_INVALID", "A Pluggy retornou uma lista de conexoes invalida.");
            }
            items.addAll((com.fasterxml.jackson.databind.node.ArrayNode) page.get("results"));
            next = page.path("next").isNull() || page.path("next").isMissingNode() ? null : page.path("next").asText();
        } while (next != null);
        return objectMapper.createObjectNode().set("results", items);
    }

    public JsonNode accounts(String itemId) {
        return get("/accounts?itemId=" + itemId + "&pageSize=500");
    }

    public JsonNode accountsByType(String itemId, String type) {
        return get("/accounts?itemId=" + itemId + "&type=" + type + "&pageSize=500");
    }

    public JsonNode transactions(String accountId, int page) {
        return get("/transactions?accountId=" + accountId + "&page=" + page + "&pageSize=500");
    }

    public JsonNode transactionsV2(String accountId, String after) {
        String suffix = after == null || after.isBlank() ? "" : "&after=" + URLEncoder.encode(after, StandardCharsets.UTF_8);
        return get("/v2/transactions?accountId=" + URLEncoder.encode(accountId, StandardCharsets.UTF_8) + suffix);
    }

    public JsonNode transactionsPage(String accountId, String next) {
        if (next == null) return transactionsV2(accountId, null);
        if (!next.startsWith("?")) throw new PluggyException("PLUGGY_PAGINATION_INVALID", "Paginacao de transacoes invalida.");
        return get("/v2/transactions" + next);
    }

    public JsonNode categories() {
        return get("/categories");
    }

    public JsonNode investments(String itemId) {
        var all=objectMapper.createArrayNode();
        for(int page=1;page<=100;page++) {
            var response=get("/investments?itemId=" + itemId + "&pageSize=500&page="+page);
            if(response==null||!response.path("results").isArray())throw new PluggyException("PLUGGY_PAYLOAD_INVALID","Resposta de investimentos inválida.");
            all.addAll((com.fasterxml.jackson.databind.node.ArrayNode)response.path("results"));
            if(page>=response.path("totalPages").asInt(1))return objectMapper.createObjectNode().set("results",all);
        }
        throw new PluggyException("PLUGGY_PAGINATION_LIMIT","Limite de páginas de investimentos excedido.");
    }

    public JsonNode bills(String accountId,int page) { return get("/bills?accountId="+accountId+"&pageSize=500&page="+page); }
    public JsonNode investmentTransactions(String investmentId,int page) { return get("/investments/"+investmentId+"/transactions?pageSize=500&page="+page); }

    private JsonNode get(String uri) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return getOnce(uri);
            } catch (PluggyException error) {
                if (attempt > 0 || !"PLUGGY_TOKEN_REJECTED".equals(error.getCode())) throw error;
                synchronized (this) {
                    cachedApiKey = null;
                }
            } catch (ResourceAccessException error) {
                throw new PluggyException("PLUGGY_UNREACHABLE", "Nao foi possivel acessar a Pluggy. Verifique a internet e tente sincronizar novamente; os dados locais foram preservados.");
            }
        }
        throw new IllegalStateException("Unreachable retry state");
    }

    private JsonNode getOnce(String uri) {
        return restClient.get()
            .uri(URI.create(BASE_URL + uri))
            .header("X-API-KEY", apiKey())
            .retrieve()
            .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                throw pluggyError("PLUGGY_REQUEST_FAILED", "A chamada para a Pluggy falhou: " + uri, clientResponse);
            })
            .body(JsonNode.class);
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        return requestFactory;
    }

    private PluggyException pluggyError(String fallbackCode, String fallbackMessage, ClientHttpResponse clientResponse) {
        try {
            String body = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!body.isBlank()) {
                JsonNode error = objectMapper.readTree(body);
                String code = clientResponse.getStatusCode().value() == 401 ? "PLUGGY_TOKEN_REJECTED" : error.path("codeDescription").asText(fallbackCode);
                if (clientResponse.getStatusCode().value() == 403 && "PLUGGY_REQUEST_FAILED".equals(code)) {
                    code = "PLUGGY_ACCESS_DENIED";
                }
                String message = switch (code) {
                    case "LIST_ITEMS_FEATURE_NOT_ENABLED" -> "A listagem de conexoes nao esta habilitada nesta conta Pluggy. Use Conectar com Pluggy: se a conta ja existir, o Aether recupera o ID informado pelo widget.";
                    case "ITEM_NOT_FOUND" -> "Esta conexao nao existe na aplicacao Pluggy configurada. Recupere a conexao existente ou conecte sua conta novamente.";
                    case "PLUGGY_ACCESS_DENIED" -> "A Pluggy negou acesso a este recurso. Verifique as permissoes da aplicacao.";
                    case "PLUGGY_AUTH_FAILED" -> "A Pluggy recusou o Client ID ou Client Secret configurado.";
                    case "PLUGGY_TOKEN_REJECTED" -> "A Pluggy recusou a autenticacao. Verifique o Client ID e Client Secret.";
                    default -> fallbackMessage;
                };
                return new PluggyException(code, message,clientResponse.getStatusCode().value());
            }
        } catch (IOException ignored) {
            // Fall back to the curated error message.
        }
        int status;
        try { status=clientResponse.getStatusCode().value(); } catch(IOException ignored) { status=0; }
        return new PluggyException(fallbackCode, fallbackMessage,status);
    }
}

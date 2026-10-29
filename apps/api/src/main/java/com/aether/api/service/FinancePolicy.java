package com.aether.api.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure rules for a local, reviewable ledger. Source amounts are never rewritten. */
public final class FinancePolicy {
    public static final Set<String> KINDS = Set.of("EXPENSE", "INCOME", "REFUND", "TRANSFER", "CARD_PAYMENT", "INVESTMENT");
    public record Classification(String kind, String reason, boolean review) {}

    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public static Classification classify(String accountType, String description, String category, String providerType, BigDecimal amount, String override) {
        if (override != null && KINDS.contains(override)) return new Classification(override, "Classificação revisada", false);
        String d = normalize(description);
        String c = normalize(category);
        String p = normalize(providerType);
        boolean card = "CREDIT".equals(accountType);
        if (p.equals("pagamento_fatura") || d.matches(".*(pagamento.*fatura|pagto.*fatura|pgto.*fat.*cart|pagamento recebido|inclusao de pagamento ciclo|pagto debito automatico).*")
            || (card && c.equals("credit card payment"))) {
            return new Classification("CARD_PAYMENT", "Pagamento de fatura; excluído dos gastos", !card && d.equals("pagto debito automatico"));
        }
        if (c.contains("same person transfer") || d.contains("mesma titularidade")) return new Classification("TRANSFER", "Transferência entre contas próprias", false);
        if (c.contains("investment") || c.equals("fixed income") || d.matches(".*(aporte na carteira|resgate na carteira|aplicacao.*cdb|cdb c6 lim|resgate.*cdb).*") ) {
            return new Classification("INVESTMENT", "Aplicação ou resgate; excluído de receitas e gastos", false);
        }
        if ((d.contains("estorno") || d.contains("devolucao") || d.contains("devol recebida")) && (card ? amount.signum()<0 : amount.signum()>0)) return new Classification("REFUND", "Estorno identificado pela descrição", true);
        // Card purchases are positive in the source; payments/refunds are usually negative.
        if (card) return amount.signum() >= 0
            ? new Classification("EXPENSE", "Compra ou encargo no cartão", false)
            : new Classification("REFUND", "Crédito no cartão: confira se é estorno ou pagamento", true);
        if (c.equals("credit card payment")) return new Classification(amount.signum() < 0 ? "EXPENSE" : "INCOME", "Categoria do provedor ambígua para conta corrente", true);
        if (c.contains("loan") || d.contains("estorno") || d.contains("devolucao")) return new Classification(amount.signum()<0?"EXPENSE":"INCOME", "Crédito, financiamento ou reversão: confira a natureza do movimento", true);
        boolean transfer = c.contains("transfer") || d.startsWith("pix") || p.equals("pix") || p.equals("ted");
        return new Classification(amount.signum() < 0 ? "EXPENSE" : "INCOME", transfer ? "PIX/TED: confira se é transferência própria" : "Movimento em conta corrente", transfer);
    }

    public static BigDecimal spending(String kind, BigDecimal amount) {
        return "EXPENSE".equals(kind) ? amount.abs() : "REFUND".equals(kind) ? amount.abs().negate() : BigDecimal.ZERO;
    }

    private static final Map<String, String> LABELS = Map.ofEntries(
        Map.entry("food and drinks", "Alimentação"), Map.entry("groceries", "Supermercado"), Map.entry("eating out", "Restaurantes"),
        Map.entry("food delivery", "Delivery"), Map.entry("shopping", "Compras"), Map.entry("clothing", "Vestuário"),
        Map.entry("transportation", "Transporte"), Map.entry("gas stations", "Combustível"), Map.entry("taxi and ride-hailing", "Transporte por aplicativo"),
        Map.entry("housing", "Moradia"), Map.entry("rent", "Aluguel"), Map.entry("electricity", "Energia"), Map.entry("water", "Água"),
        Map.entry("healthcare", "Saúde"), Map.entry("pharmacy", "Farmácia"), Map.entry("hospital clinics and labs", "Clínicas e exames"),
        Map.entry("education", "Educação"), Map.entry("university", "Universidade"), Map.entry("leisure", "Lazer"), Map.entry("travel", "Viagens"),
        Map.entry("services", "Serviços"), Map.entry("digital services", "Serviços digitais"), Map.entry("telecommunications", "Telefonia e internet"),
        Map.entry("music streaming", "Streaming de música"), Map.entry("video streaming", "Streaming de vídeo"), Map.entry("online shopping", "Compras online"),
        Map.entry("salary", "Salário"), Map.entry("income", "Receitas"), Map.entry("other", "Outros"), Map.entry("cashback", "Cashback"),
        Map.entry("credit card payment", "Pagamento de cartão"), Map.entry("transfers", "Transferências"), Map.entry("transfer - pix", "PIX"),
        Map.entry("same person transfer", "Transferência própria"), Map.entry("same person transfer - pix", "PIX entre contas próprias"),
        Map.entry("transfer - internal", "Transferência interna"), Map.entry("investments", "Investimentos"), Map.entry("automatic investment", "Aplicação automática"),
        Map.entry("proceeds interests and dividends", "Juros e rendimentos"), Map.entry("loans", "Empréstimos"), Map.entry("bookstore", "Livraria"),
        Map.entry("gyms and fitness centers", "Academia"), Map.entry("wellness and fitness", "Bem-estar"), Map.entry("taxes", "Impostos"),
        Map.entry("vehicle maintenance", "Manutenção de veículo"), Map.entry("electronics", "Eletrônicos"), Map.entry("houseware", "Casa"),
        Map.entry("pet supplies and vet", "Pets"), Map.entry("automotive", "Veículos"), Map.entry("insurance", "Seguros")
    );
    public static String label(String category) { return category == null || category.isBlank() ? "Sem categoria" : LABELS.getOrDefault(normalize(category), category); }
    private FinancePolicy() {}
}

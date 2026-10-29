package com.aether.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionDetailsTests {
    private final ObjectMapper mapper=new ObjectMapper();
    private TransactionDetails.Identity identity(String payload,String manual,String description,String type,String amount) throws Exception {
        return TransactionDetails.identity(mapper.readTree(payload),description,type,new BigDecimal(amount),manual,"EXPENSE");
    }
    @Test void usesMerchantThenTheCorrectCounterpartyWithoutConfusingPayerWithRecipient() throws Exception {
        var payload="{\"paymentData\":{\"payer\":{\"name\":\"Eu\"},\"receiver\":{\"name\":\"UFFA\"}}}";
        assertThat(identity(payload,null,"Débito de Cartão","BANK","-10.98").name()).isEqualTo("UFFA");
        assertThat(identity(payload,null,"PIX recebido","BANK","10.98").name()).isEqualTo("Eu");
        assertThat(identity(payload,"Correção local","Débito de Cartão","BANK","-10.98").source()).isEqualTo("manual");
        assertThat(identity("{\"merchant\":{\"businessName\":\"Loja Ltda\"}}",null,"Compra","BANK","-10").name()).isEqualTo("Loja Ltda");
    }
    @Test void genericDescriptionNeedsIdentificationEvenWhenCategoryOrOwnNameExists() throws Exception {
        var missing=identity("{\"category\":\"Shopping\",\"paymentData\":{\"payer\":{\"name\":\"Eu\"}}}",null,"Débito de Cartão","BANK","-10.98");
        assertThat(missing.status()).isEqualTo("missing");assertThat(missing.name()).isNull();
        var partial=identity("{\"paymentData\":{\"receiver\":{\"documentNumber\":{\"value\":\"123\"}}}}",null,"PIX enviado","BANK","-10");
        assertThat(partial.status()).isEqualTo("document_only");
    }
    @Test void originalDescriptionCanSupplyTheNameAndKnownFinancialMovementsNeedNoInventedMerchant() throws Exception {
        assertThat(identity("{\"descriptionRaw\":\"UFFA Santa Rosa\"}",null,"Débito de Cartão","BANK","-10").source()).isEqualTo("original_description");
        assertThat(TransactionDetails.identity(mapper.readTree("{}"),"Pagamento recebido","CREDIT",BigDecimal.TEN,null,"CARD_PAYMENT").status()).isEqualTo("not_required");
    }
    @Test void convertsOffsetTimestampsAndPreservesPurchaseTimeAcrossTheCalendarBoundary() throws Exception {
        var time=TransactionDetails.timing(mapper.readTree("{\"date\":\"2026-10-23T22:08:12Z\"}"));
        assertThat(time.localTime()).isEqualTo("19:08");assertThat(time.source()).isEqualTo("posted");
        var purchase=TransactionDetails.timing(mapper.readTree("{\"date\":\"2026-10-24T00:00:00Z\",\"creditCardMetadata\":{\"transactionDateTime\":\"2026-10-24T01:30:00Z\"}}"));
        assertThat(purchase.localDate()).isEqualTo("2026-10-23");assertThat(purchase.localTime()).isEqualTo("22:30");assertThat(purchase.source()).isEqualTo("purchase");
    }
    @Test void doesNotInventTimesFromDateOnlyMidnightOrMalformedValues() throws Exception {
        for(String date:new String[]{"2026-10-23","2026-10-23T00:00:00.000Z","2026-10-23T03:00:00Z","invalid"})
            assertThat(TransactionDetails.timing(mapper.createObjectNode().put("date",date)).localTime()).isNull();
        assertThat(TransactionDetails.timing(mapper.readTree("{\"creditCardMetadata\":{\"transactionDateTime\":\"2026-10-23T00:00:00Z\"}}")).localTime()).isEqualTo("21:00");
    }
}

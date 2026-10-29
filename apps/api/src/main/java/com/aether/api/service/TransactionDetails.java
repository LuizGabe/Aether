package com.aether.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Presentation metadata derived from the source; never guesses a merchant or a time. */
public final class TransactionDetails {
    private static final ZoneId ZONE=ZoneId.of("America/Sao_Paulo");
    private static final Set<String> GENERIC=Set.of("debito de cartao","pagamento em debito automatico",
        "pagamento a um lojista de e-commerce","pagamento de boleto","boleto estornado/liquidado apos data de vencimento",
        "creditos utilizados em transacao","estorno de compra feita com cartao picpay","recarga","pix enviado","pix recebido",
        "pagamento on line","academia","pagamento de debito veicular","sem descricao","transferencia","pix","compra","outros");
    public record Identity(String name,String source,String status,boolean documentPresent) {}
    public record Timing(String timestamp,String localDate,String localTime,String source) {}
    public static Identity identity(JsonNode raw,String description,String accountType,BigDecimal amount,String manual,String kind) {
        boolean incoming="CREDIT".equals(accountType)?amount.signum()<0:amount.signum()>0;
        var party=raw.path("paymentData").path(incoming?"payer":"receiver");
        boolean document=!value(party.path("documentNumber"),"value").isBlank();
        if(manual!=null&&!manual.isBlank())return new Identity(manual,"manual","identified",document);
        String name=first(value(raw.path("merchant"),"name"),value(raw.path("merchant"),"businessName"));
        if(!name.isBlank())return new Identity(name,"merchant","identified",document);
        name=value(party,"name");
        if(!name.isBlank())return new Identity(name,"payment","identified",document);
        if(Set.of("CARD_PAYMENT","INVESTMENT").contains(kind))return new Identity(null,"financial_movement","not_required",document);
        String original=value(raw,"descriptionRaw");
        if(meaningful(original))return new Identity(original,"original_description","identified",document);
        if(meaningful(description))return new Identity(description,"description","identified",document);
        String reason=value(raw.path("paymentData"),"reason");
        if(meaningful(reason))return new Identity(reason,"payment_reason","identified",document);
        String info=value(raw,"operationTypeAdditionalInfo");
        if(meaningful(info)&&!Set.of("outro","other","recebimento via pix").contains(FinancePolicy.normalize(info)))
            return new Identity(info,"additional_info","identified",document);
        return new Identity(null,"unavailable",document?"document_only":"missing",document);
    }
    private static boolean meaningful(String s){String n=FinancePolicy.normalize(s);return !n.isBlank()&&!GENERIC.contains(n)&&!Set.of("outro","other","null","n/a").contains(n);}
    public static Timing timing(JsonNode raw) {
        String purchase=value(raw.path("creditCardMetadata"),"transactionDateTime");
        var exact=parse(purchase);
        if(exact!=null&&!exact.atZoneSameInstant(ZONE).toLocalTime().equals(LocalTime.MIDNIGHT))return timing(exact,"purchase");
        String timestamp=value(raw,"date");
        var posted=parse(timestamp);
        // Providers often synthesize 00:00 UTC for date-only records. Do not display this as 21:00.
        if(posted!=null&&!posted.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime().equals(LocalTime.MIDNIGHT)&&!posted.atZoneSameInstant(ZONE).toLocalTime().equals(LocalTime.MIDNIGHT))return timing(posted,"posted");
        return new Timing(null,null,null,"unavailable");
    }
    private static Timing timing(OffsetDateTime v,String source){var local=v.atZoneSameInstant(ZONE);return new Timing(v.toInstant().toString(),local.toLocalDate().toString(),local.format(DateTimeFormatter.ofPattern("HH:mm")),source);}
    private static OffsetDateTime parse(String v){try{return OffsetDateTime.parse(v);}catch(Exception e){return null;}}
    private static String value(JsonNode n,String k){var v=n.path(k);return v.isTextual()?v.asText().trim():"";}
    private static String first(String... values){for(String v:values)if(!v.isBlank())return v;return "";}
    private TransactionDetails(){}
}

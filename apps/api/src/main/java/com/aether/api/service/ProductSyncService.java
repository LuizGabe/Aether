package com.aether.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductSyncService {
    public record Result(int bills,int movements,List<String> warnings) {}
    private final JdbcTemplate jdbc;
    private final PluggyClient client;
    private final ObjectMapper mapper;
    private final WealthSnapshotService wealth;
    public ProductSyncService(JdbcTemplate jdbc,PluggyClient client,ObjectMapper mapper,WealthSnapshotService wealth){this.jdbc=jdbc;this.client=client;this.mapper=mapper;this.wealth=wealth;}

    public Result sync(String connectionId) {
        int bills=0,movements=0;var warnings=new ArrayList<String>();
        for(var account:jdbc.queryForList("SELECT id,external_account_id FROM accounts WHERE connection_id=? AND subtype='CREDIT_CARD'",connectionId)) {
            try {
                for(int page=1;page<=100;page++) {
                    var data=client.bills(account.get("external_account_id").toString(),page);
                    if(data==null)break;
                    for(var bill:data.path("results")) {
                        if(!bill.hasNonNull("id"))continue;
                        jdbc.update("""
                            INSERT INTO card_bills(id,account_id,due_date,closing_date,amount,currency_code,minimum_payment,raw_payload)
                            VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET due_date=excluded.due_date,closing_date=excluded.closing_date,
                                amount=excluded.amount,currency_code=excluded.currency_code,minimum_payment=excluded.minimum_payment,
                                raw_payload=excluded.raw_payload,updated_at=CURRENT_TIMESTAMP
                            """,bill.path("id").asText(),account.get("id"),date(bill,"dueDate"),date(bill,"billClosingDate"),dec(bill,"totalAmount"),
                            bill.path("totalAmountCurrencyCode").asText("BRL"),dec(bill,"minimumPaymentAmount"),bill.toString());bills++;
                    }
                    if(page>=data.path("totalPages").asInt(1))break;
                    if(page==100)warnings.add("Faturas: limite de páginas atingido.");
                }
            }catch(PluggyException e){warnings.add("Faturas: "+productError(e));}
        }
        for(var investment:jdbc.queryForList("SELECT id,external_investment_id,raw_payload,currency_code FROM investments WHERE connection_id=?",connectionId)) {
            JsonNode raw;
            try{raw=mapper.readTree(investment.get("raw_payload").toString());}catch(Exception e){continue;}
            jdbc.update("""
                INSERT INTO investment_snapshots(investment_id,snapshot_date,source_date,gross_amount,net_balance,currency_code)
                VALUES (?,?,?,?,?,?) ON CONFLICT(investment_id,snapshot_date) DO UPDATE SET source_date=excluded.source_date,
                    gross_amount=excluded.gross_amount,net_balance=excluded.net_balance,currency_code=excluded.currency_code
                """,investment.get("id"),LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString(),date(raw,"date"),dec(raw,"amount"),dec(raw,"balance"),investment.get("currency_code"));
            try {
                for(int page=1;page<=100;page++) {
                    var data=client.investmentTransactions(investment.get("external_investment_id").toString(),page);
                    if(data==null)break;
                    for(var movement:data.path("results")) {
                        if(!movement.hasNonNull("id"))continue;
                        jdbc.update("""
                            INSERT INTO investment_movements(id,investment_id,date,type,description,amount,net_amount,quantity,raw_payload)
                            VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET date=excluded.date,type=excluded.type,
                                description=excluded.description,amount=excluded.amount,net_amount=excluded.net_amount,quantity=excluded.quantity,
                                raw_payload=excluded.raw_payload,updated_at=CURRENT_TIMESTAMP
                            """,movement.path("id").asText(),investment.get("id"),date(movement,"date"),text(movement,"type"),text(movement,"description"),
                            dec(movement,"amount"),dec(movement,"netAmount"),dec(movement,"quantity"),movement.toString());movements++;
                    }
                    if(page>=data.path("totalPages").asInt(1))break;
                    if(page==100)warnings.add("Movimentações de investimentos: limite de páginas atingido.");
                }
            }catch(PluggyException e){warnings.add("Movimentações de investimentos: "+productError(e));}
        }
        wealth.recordDaily();
        return new Result(bills,movements,warnings.stream().distinct().toList());
    }
    private String productError(PluggyException e){return e.getHttpStatus()==403||e.getHttpStatus()==404?"produto indisponível ou sem permissão nesta conexão.":e.getMessage();}
    private static String text(JsonNode n,String key){var v=n.path(key);return v.isMissingNode()||v.isNull()?null:v.asText();}
    private static String date(JsonNode n,String key){String v=text(n,key);return v==null||v.length()<10?null:v.substring(0,10);}
    private static BigDecimal dec(JsonNode n,String key){try{String v=text(n,key);return v==null?null:new BigDecimal(v);}catch(Exception e){return null;}}
}

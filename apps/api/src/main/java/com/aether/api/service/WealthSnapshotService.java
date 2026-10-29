package com.aether.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WealthSnapshotService {
    private final JdbcTemplate jdbc;
    public WealthSnapshotService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public void recordDaily(){
        BigDecimal available=jdbc.queryForObject("SELECT COALESCE(SUM(balance),0) FROM accounts WHERE type='BANK' AND currency_code='BRL'",BigDecimal.class);
        BigDecimal cards=jdbc.queryForObject("SELECT COALESCE(SUM(balance),0) FROM accounts WHERE subtype='CREDIT_CARD' AND currency_code='BRL'",BigDecimal.class);
        BigDecimal invested=jdbc.queryForObject("SELECT COALESCE(SUM(json_extract(raw_payload,'$.balance')),0) FROM investments WHERE currency_code='BRL' AND COALESCE(json_extract(raw_payload,'$.status'),'ACTIVE') NOT IN ('TOTAL_WITHDRAWAL','REDEEMED','CLOSED')",BigDecimal.class);
        Integer missing=jdbc.queryForObject("SELECT COUNT(*) FROM investments WHERE currency_code='BRL' AND json_extract(raw_payload,'$.balance') IS NULL AND (COALESCE(amount,0)>0 OR COALESCE(quantity,0)>0)",Integer.class);
        jdbc.update("""
            INSERT INTO wealth_snapshots(snapshot_date,available,invested,card_balance,net_worth,incomplete_positions)
            VALUES (?,?,?,?,?,?) ON CONFLICT(snapshot_date) DO UPDATE SET available=excluded.available,invested=excluded.invested,
                card_balance=excluded.card_balance,net_worth=excluded.net_worth,incomplete_positions=excluded.incomplete_positions
            """,LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString(),available,invested,cards,available.add(invested).subtract(cards),missing);
    }
}

package com.aether.api.service;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties="spring.datasource.url=jdbc:sqlite:file:workspace-test?mode=memory&cache=shared")
class WorkspaceServiceTests {
    @Autowired WorkspaceService service;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void setup(){
        for(String t:List.of("visual_preferences","wealth_snapshots","classification_rules","card_bills","investment_movements","investment_snapshots","sync_jobs","transactions","investments","accounts","connections","institutions","categories"))jdbc.update("DELETE FROM "+t);
        jdbc.update("INSERT INTO connections(id,display_name,status) VALUES ('bank','Banco','UPDATED')");
        jdbc.update("INSERT INTO categories(id,name,kind,system) VALUES ('cat','Groceries','expense',1)");
        jdbc.update("INSERT INTO accounts(id,connection_id,type,subtype,name,balance,raw_payload) VALUES ('cash','bank','BANK','CHECKING_ACCOUNT','Conta',1000,'{}')");
        jdbc.update("INSERT INTO accounts(id,connection_id,type,subtype,name,balance,raw_payload) VALUES ('card','bank','CREDIT','CREDIT_CARD','Cartão',200,?)","{\"creditData\":{\"creditLimit\":1000,\"availableCreditLimit\":800,\"balanceDueDate\":\"2026-10-10\"}}");
        entry("purchase-bank","cash","Mercado","Groceries","-100");
        entry("purchase-card","card","Mercado","Groceries","100");
        entry("payment-bank","cash","PAGAMENTO FATURA INTER","Credit card payment","-100");
        entry("payment-card","card","Pagamento recebido","Credit card payment","-100");
        entry("own","cash","PIX próprio","Same person transfer","500");
        entry("income","cash","Salário","Salary","1000");
        entry("investment","cash","CDB C6 LIM.GARANT.","Automatic investment","-450");
        entry("refund","card","Crédito","Shopping","-20");
        jdbc.update("INSERT INTO investments(id,connection_id,name,type,value,amount,raw_payload) VALUES ('asset','bank','CDB','FIXED_INCOME',1.02972928,463.37,?)","{\"status\":\"ACTIVE\",\"quantity\":450,\"value\":1.02972928,\"amount\":463.37,\"balance\":460.36,\"date\":\"2026-09-30\"}");
    }
    private void entry(String id,String account,String description,String category,String amount){jdbc.update("INSERT INTO transactions(id,account_id,connection_id,date,description,amount,pluggy_category,status,raw_payload) VALUES (?,?,'bank','2026-09-20',?,?,?,'POSTED','{}')",id,account,description,new BigDecimal(amount),category);}
    @SuppressWarnings("unchecked") private Map<String,Object> summary(){return (Map<String,Object>)service.workspace("2026-09").get("summary");}
    @Test void dashboardSeparatesCashFromConsumptionAndInvestments(){
        var s=summary();
        assertThat((BigDecimal)s.get("available")).isEqualByComparingTo("1000");
        assertThat((BigDecimal)s.get("income")).isEqualByComparingTo("1000");
        assertThat((BigDecimal)s.get("expenses")).isEqualByComparingTo("180");
        assertThat((BigDecimal)s.get("cashIn")).isEqualByComparingTo("1500");
        assertThat((BigDecimal)s.get("cashOut")).isEqualByComparingTo("650");
        assertThat((BigDecimal)s.get("investedNet")).isEqualByComparingTo("460.36");
        assertThat((BigDecimal)s.get("netWorth")).isEqualByComparingTo("1260.36");
    }
    @Test @SuppressWarnings("unchecked") void cardsAndInvestmentsExposeCorrectMetadata(){
        var w=service.workspace("2026-09");
        var accounts=(List<Map<String,Object>>)w.get("accounts");
        var card=accounts.stream().filter(a -> "CARD".equals(a.get("kind"))).findFirst().orElseThrow();
        assertThat((BigDecimal)card.get("limit")).isEqualByComparingTo("1000");
        var investments=(Map<String,Object>)w.get("investments");var positions=(List<Map<String,Object>>)investments.get("positions");
        assertThat((BigDecimal)positions.getFirst().get("grossAmount")).isEqualByComparingTo("463.37");
        assertThat((BigDecimal)positions.getFirst().get("unitValue")).isEqualByComparingTo("1.02972928");
    }
    @Test @SuppressWarnings("unchecked") void paginationCanReachBeyondLegacyFiveHundredRowsAndFiltersBills(){
        for(int i=0;i<505;i++)entry("extra-"+i,"cash","Outra compra","Shopping","-1");
        var page=service.transactions(Map.of("page","6","pageSize","100"));
        assertThat(page.get("total")).isEqualTo(513);assertThat((List<?>)page.get("items")).hasSize(13);
        jdbc.update("UPDATE transactions SET raw_payload=? WHERE id='purchase-card'","{\"creditCardMetadata\":{\"billId\":\"bill-one\"}}");
        assertThat(service.transactions(Map.of("billId","bill-one")).get("total")).isEqualTo(1);
    }
    @Test @SuppressWarnings("unchecked") void manualEditsOverrideRulesAndBatchIsAtomic(){
        service.editTransaction("purchase-bank",Map.of("kind","TRANSFER","notes","Revisado"));
        service.saveRule(Map.of("pattern","Mercado","kind","EXPENSE"));
        var rows=(List<Map<String,Object>>)service.transactions(Map.of("q","Mercado")).get("items");
        assertThat(rows.stream().filter(r -> r.get("id").equals("purchase-bank")).findFirst().orElseThrow().get("kind")).isEqualTo("TRANSFER");
        assertThatThrownBy(() -> service.batch(Map.of("ids",List.of("purchase-card","missing"),"kind","TRANSFER"))).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT financial_kind_override FROM transactions WHERE id='purchase-card'",String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT amount FROM transactions WHERE id='purchase-bank'",BigDecimal.class)).isEqualByComparingTo("-100");
    }
    @Test void pendingAndIgnoredTransactionsStayOutsideTotals(){
        jdbc.update("UPDATE transactions SET status='PENDING' WHERE id='purchase-card'");
        service.editTransaction("purchase-bank",Map.of("ignored",true));
        assertThat((BigDecimal)summary().get("expenses")).isEqualByComparingTo("-20");
    }
    @Test void paymentAccountMustBeABankAccount(){
        assertThatThrownBy(() -> service.editAccount("card",Map.of("paymentAccountId","card"))).isInstanceOf(IllegalArgumentException.class);
        service.editAccount("card",Map.of("paymentAccountId","cash"));
        assertThat(jdbc.queryForObject("SELECT payment_account_id FROM accounts WHERE id='card'",String.class)).isEqualTo("cash");
    }
    @Test void backupProducesAConsistentSqliteFile(){
        byte[] backup=service.backup();
        assertThat(new String(backup,0,15,java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("SQLite format 3");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM transactions",Integer.class)).isEqualTo(8);
    }
    @Test @SuppressWarnings("unchecked") void installmentForecastUsesOnlyBookedDomesticPurchases(){
        entry("parcel","card","Notebook","Shopping","120");
        String metadata="{\"creditCardMetadata\":{\"installmentNumber\":2,\"totalInstallments\":4,\"purchaseDate\":\"2026-08-20\"}}";
        jdbc.update("UPDATE transactions SET raw_payload=? WHERE id='parcel'",metadata);
        entry("foreign","card","Foreign","Shopping","900");
        jdbc.update("UPDATE transactions SET raw_payload=?,currency_code='USD' WHERE id='foreign'",metadata);
        entry("future","card","Future","Shopping","800");
        jdbc.update("UPDATE transactions SET raw_payload=?,status='PENDING' WHERE id='future'",metadata);
        var forecast=(List<Map<String,Object>>)service.workspace("2026-09").get("forecast");
        assertThat((BigDecimal)forecast.get(0).get("amount")).isEqualByComparingTo("120");
        assertThat((BigDecimal)forecast.get(1).get("amount")).isEqualByComparingTo("120");
        assertThat((BigDecimal)forecast.get(2).get("amount")).isZero();
    }
    @Test @SuppressWarnings("unchecked") void ambiguousProviderCategoryDoesNotPresentDebitPurchasesAsBillPayments(){
        entry("ambiguous","cash","Débito de Cartão","Credit card payment","-30");
        var rows=(List<Map<String,Object>>)service.transactions(Map.of("q","Débito de Cartão")).get("items");
        assertThat(rows.getFirst().get("kind")).isEqualTo("EXPENSE");
        assertThat(rows.getFirst().get("categoryName")).isEqualTo("Categoria a revisar");
        assertThat(rows.getFirst().get("review")).isEqualTo(true);
    }
    @Test @SuppressWarnings("unchecked") void identificationIsReviewableSearchableAndIndependentOfFinancialClassification(){
        entry("unidentified","cash","Débito de Cartão","Groceries","-10.98");
        jdbc.update("UPDATE transactions SET raw_payload=? WHERE id='unidentified'","{\"date\":\"2026-09-20T22:08:12Z\"}");
        var rows=(List<Map<String,Object>>)service.transactions(Map.of("origin","incomplete","q","Débito")).get("items");
        assertThat(rows).hasSize(1);assertThat(rows.getFirst().get("localTime")).isEqualTo("19:08");
        service.editTransaction("unidentified",Map.of("counterparty","UFFA"));
        var revised=(List<Map<String,Object>>)service.transactions(Map.of("q","UFFA","origin","manual")).get("items");
        assertThat(revised).hasSize(1);assertThat(revised.getFirst().get("originStatus")).isEqualTo("identified");
        assertThat(revised.getFirst().get("kind")).isEqualTo("EXPENSE");
        assertThat(jdbc.queryForObject("SELECT amount FROM transactions WHERE id='unidentified'",BigDecimal.class)).isEqualByComparingTo("-10.98");
        service.editTransaction("unidentified",Collections.singletonMap("counterparty",null));
        assertThat(service.transactions(Map.of("q","UFFA")).get("total")).isEqualTo(0);
        assertThat(service.transactions(Map.of("origin","incomplete","q","Débito")).get("total")).isEqualTo(1);
    }
    @Test @SuppressWarnings("unchecked") void visualPreferencesPreserveOriginalDataAndCanBeReset() throws Exception {
        var picture=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(picture,"png",bytes);
        String image="data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes.toByteArray());
        service.saveVisualPreference(Map.of("scope","institution","targetId","bank","alias","Meu banco","image",image));
        assertThat(service.visualPreferences()).hasSize(1);
        assertThat(service.visualPreferences().getFirst().get("alias")).isEqualTo("Meu banco");
        assertThat(service.visualPreferences().getFirst().get("image")).isEqualTo(image);
        assertThat(jdbc.queryForObject("SELECT display_name FROM connections WHERE id='bank'",String.class)).isEqualTo("Banco");
        assertThat(summary().get("available")).isEqualTo(new BigDecimal("1000"));
        service.saveVisualPreference(Map.of("scope","institution","targetId","bank","alias","","image",""));
        assertThat(service.visualPreferences().getFirst().get("alias")).isNull();
        assertThat(service.visualPreferences().getFirst().get("image")).isNull();
        assertThatThrownBy(()->service.saveVisualPreference(Map.of("scope","institution","targetId","bank","image","data:image/png;base64,YWJj"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.saveVisualPreference(Map.of("scope","institution","targetId","missing","alias","X"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.saveVisualPreference(Map.of("scope","institution","targetId","bank","image","https://example.com/a.png"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test @SuppressWarnings("unchecked") void recurrenceIdentitySurvivesNewMonthsAndKeepsAccountsSeparate() {
        for(int month=7;month<=9;month++){
            entry("subscription-"+month,"cash","STREAMING 123","Shopping","-30");
            jdbc.update("UPDATE transactions SET date=? WHERE id=?",String.format("2026-%02d-20",month),"subscription-"+month);
        }
        var before=(List<Map<String,Object>>)service.workspace("2026-09").get("recurrences");
        assertThat(before).hasSize(1);String id=before.getFirst().get("id").toString();
        service.saveVisualPreference(Map.of("scope","recurrence","targetId",id,"alias","Meu streaming"));
        entry("subscription-10","cash","STREAMING 456","Shopping","-31");
        jdbc.update("UPDATE transactions SET date='2026-10-20' WHERE id='subscription-10'");
        var after=(List<Map<String,Object>>)service.workspace("2026-10").get("recurrences");
        assertThat(after.getFirst().get("id")).isEqualTo(id);
        assertThat(service.visualPreferences().getFirst().get("alias")).isEqualTo("Meu streaming");
        assertThat(jdbc.queryForObject("SELECT description FROM transactions WHERE id='subscription-10'",String.class)).isEqualTo("STREAMING 456");
    }
}

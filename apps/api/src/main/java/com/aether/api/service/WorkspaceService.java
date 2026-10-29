package com.aether.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class WorkspaceService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public WorkspaceService(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    public Map<String, Object> workspace(String month) {
        YearMonth selected = parseMonth(month);
        var accounts = accounts();
        var entries = entries();
        var positions = positions();
        var active = positions.stream().filter(p -> Boolean.TRUE.equals(p.get("active"))).toList();
        var monthly = booked(entries).stream().filter(t -> s(t,"date").startsWith(selected.toString())).toList();
        var groups = new HashMap<String, BigDecimal>();
        for (var t : monthly) {
            var spend = FinancePolicy.spending(s(t,"kind"), n(t,"amount"));
            if (spend.signum() != 0) groups.merge(s(t,"categoryName"), spend, BigDecimal::add);
        }
        var spending = groups.entrySet().stream().filter(e -> e.getValue().signum()>0)
            .sorted(Map.Entry.<String,BigDecimal>comparingByValue().reversed())
            .map(e -> obj("name", e.getKey(), "amount", e.getValue())).toList();
        var series = new ArrayList<Map<String,Object>>();
        for (int i=5;i>=0;i--) {
            String ym = selected.minusMonths(i).toString();
            var ts = booked(entries).stream().filter(t -> s(t,"date").startsWith(ym)).toList();
            series.add(obj("month",ym,"income",income(ts),"expenses",expenses(ts),"cashIn",cash(ts,true),"cashOut",cash(ts,false)));
        }
        var summary = obj("available",sum(accounts.stream().filter(a -> "BANK".equals(a.get("kind")) && "BRL".equals(a.get("currency"))).toList(),"balance"),
            "investedGross",sum(active.stream().filter(p -> "BRL".equals(p.get("currency"))).toList(),"grossAmount"),
            "investedNet",sum(active.stream().filter(p -> "BRL".equals(p.get("currency"))).toList(),"netBalance"),
            "investmentUnknown",active.stream().filter(p -> p.get("netBalance")==null).count(),
            "income",income(monthly),"expenses",expenses(monthly),"cashIn",cash(monthly,true),"cashOut",cash(monthly,false),
            "reviewCount",monthly.stream().filter(t -> Boolean.TRUE.equals(t.get("review")) && !Boolean.TRUE.equals(t.get("ignored"))).count(),
            "bankCount",accounts.stream().filter(a -> "BANK".equals(a.get("kind"))).count(),
            "cardCount",accounts.stream().filter(a -> "CARD".equals(a.get("kind"))).count(),"transactionCount",entries.size());
        BigDecimal cardBalance=sum(accounts.stream().filter(a -> "CARD".equals(a.get("kind"))&&"BRL".equals(a.get("currency"))).toList(),"balance");
        summary.put("originReviewCount",monthly.stream().filter(t -> Set.of("missing","document_only").contains(s(t,"originStatus"))).count());
        summary.put("cardBalance",cardBalance);
        summary.put("netWorth",((BigDecimal)summary.get("available")).add((BigDecimal)summary.get("investedNet")).subtract(cardBalance));
        var connections = jdbc.queryForList("SELECT id, display_name AS name, status, execution_status AS executionStatus, last_success_at AS lastSuccessAt, last_error_message AS error FROM connections ORDER BY updated_at DESC");
        for (var c : connections) {
            c.put("lastTransaction", entries.stream().filter(t -> Objects.equals(t.get("connectionId"),c.get("id")))
                .map(t -> s(t,"date")).max(String::compareTo).orElse(null));
        }
        return obj("firstRun",accounts.isEmpty() && connections.isEmpty() && entries.isEmpty(),"month",selected.toString(),"summary",summary,"accounts",accounts,"categories",categories(),"connections",connections,
            "categorySpending",spending,"series",series,"recurrences",recurrences(entries,selected),"forecast",forecast(entries,selected),
            "recent",monthly.stream().limit(7).toList(),"investments",obj("positions",positions,"movements",investmentMovements(),"history",investmentHistory()),
            "bills",bills(),"rules",rules(),"visualPreferences",visualPreferences(),"wealthHistory",jdbc.queryForList("SELECT snapshot_date AS date,available,invested,card_balance AS cardBalance,net_worth AS netWorth,incomplete_positions AS incompletePositions FROM wealth_snapshots ORDER BY snapshot_date DESC LIMIT 366").reversed());
    }

    public List<Map<String,Object>> accounts() {
        var result = new ArrayList<Map<String,Object>>();
        for (var row : jdbc.queryForList("""
            SELECT a.*, c.display_name AS institution, c.last_success_at AS synced, c.raw_payload AS connection_payload,
                p.display_name AS payment_name, p.name AS payment_source_name
            FROM accounts a LEFT JOIN connections c ON c.id=a.connection_id
            LEFT JOIN accounts p ON p.id=a.payment_account_id ORDER BY institution,a.type,a.name
            """)) {
            JsonNode raw=json(row.get("raw_payload")), credit=raw.path("creditData"), bank=raw.path("bankData"), connection=json(row.get("connection_payload"));
            String kind = "BANK".equals(row.get("type")) ? "BANK" : "CREDIT_CARD".equals(row.get("subtype")) ? "CARD" : "OTHER";
            String number=s(row,"number_masked");
            result.add(obj("id",row.get("id"),"connectionId",row.get("connection_id"),"kind",kind,"type",row.get("type"),
                "name",first(row.get("display_name"), row.get("marketing_name"),row.get("name"),kind.equals("CARD")?"Cartão":"Conta corrente"),
                "institution",row.get("institution"),"number",number.isBlank()?null:"•••• "+number.substring(Math.max(0,number.length()-4)),
                "balance",decimal(row.get("balance")),"currency",row.get("currency_code"),"lastSyncedAt",row.get("synced"),
                "sourceUpdatedAt",text(connection,"lastUpdatedAt"),"limit",dec(credit,"creditLimit"),"availableLimit",dec(credit,"availableCreditLimit"),
                "closeDate",date(credit,"balanceCloseDate"),"dueDate",date(credit,"balanceDueDate"),"brand",text(credit,"brand"),
                "automaticInvestment",dec(bank,"automaticallyInvestedBalance"),"paymentAccountId",row.get("payment_account_id"),
                "paymentAccountName",first(row.get("payment_name"),row.get("payment_source_name"))));
        }
        return result;
    }

    public List<Map<String,Object>> visualPreferences() {
        return jdbc.queryForList("SELECT scope,target_id AS targetId,alias,image FROM visual_preferences");
    }

    public void saveVisualPreference(Map<String,Object> body) {
        String scope=Objects.toString(body.get("scope"),""), target=Objects.toString(body.get("targetId"),"");
        if(!Set.of("institution","recurrence").contains(scope)||target.isBlank()||target.length()>120)
            throw new IllegalArgumentException("Preferência visual inválida.");
        if(scope.equals("institution") && jdbc.queryForObject("SELECT count(*) FROM connections WHERE id=?",Integer.class,target)==0)
            throw new IllegalArgumentException("Instituição não encontrada.");
        String alias=Objects.toString(body.get("alias"),"").trim(), image=Objects.toString(body.get("image"),"");
        if(alias.length()>100)throw new IllegalArgumentException("O apelido deve ter até 100 caracteres.");
        if(!image.isBlank()) {
            if(image.length()>350000||!image.matches("data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+"))
                throw new IllegalArgumentException("Escolha uma imagem PNG, JPG ou WebP de até 250 KB.");
            try {
                byte[] bytes=Base64.getDecoder().decode(image.substring(image.indexOf(',')+1));
                if(bytes.length>250000)throw new IllegalArgumentException();
                var decoded=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                if(decoded==null||decoded.getWidth()>1024||decoded.getHeight()>1024)throw new IllegalArgumentException();
            }catch(Exception e){throw new IllegalArgumentException("Imagem inválida. Use PNG ou JPG de até 1024 pixels.");}
        }
        jdbc.update("INSERT INTO visual_preferences(scope,target_id,alias,image) VALUES (?,?,?,?) ON CONFLICT(scope,target_id) DO UPDATE SET alias=excluded.alias,image=excluded.image",
            scope,target,alias.isBlank()?null:alias,image.isBlank()?null:image);
    }

    public byte[] backup() {
        java.nio.file.Path directory=null;
        try {
            directory=java.nio.file.Files.createTempDirectory("aether-backup-");
            var file=directory.resolve("aether.sqlite");
            jdbc.update("VACUUM INTO ?",file.toString());
            return java.nio.file.Files.readAllBytes(file);
        } catch(java.io.IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Não foi possível criar o backup local.");
        } finally {
            if(directory!=null)try{java.nio.file.Files.deleteIfExists(directory.resolve("aether.sqlite"));java.nio.file.Files.deleteIfExists(directory);}catch(java.io.IOException ignored){}
        }
    }

    public Map<String,Object> transactions(Map<String,String> filters) {
        int page=positive(filters.getOrDefault("page","1"),1,1_000_000), size=positive(filters.getOrDefault("pageSize","40"),1,100);
        String from=filters.get("from"),to=filters.get("to");
        if (from!=null && !from.isBlank()) LocalDate.parse(from);
        if (to!=null && !to.isBlank()) LocalDate.parse(to);
        if (from!=null && to!=null && !from.isBlank() && !to.isBlank() && from.compareTo(to)>0) throw new IllegalArgumentException("A data inicial deve ser anterior à final.");
        var all=entries().stream().filter(t -> matches(t,filters)).toList();
        int start=(int)Math.min(all.size(),(long)(page-1)*size);
        return obj("items",all.subList(start,Math.min(start+size,all.size())),"total",all.size(),"page",page,"pageSize",size,
            "pages",Math.max(1,(all.size()+size-1)/size),"totals",obj("income",income(booked(all)),"expenses",expenses(booked(all))));
    }

    private boolean matches(Map<String,Object> t,Map<String,String> f) {
        String q=FinancePolicy.normalize(f.get("q"));
        if (!q.isBlank() && !FinancePolicy.normalize(s(t,"description")+" "+s(t,"counterparty")+" "+s(t,"institution")+" "+s(t,"categoryName")).contains(q)) return false;
        if ("incomplete".equals(f.get("origin"))&&!Set.of("missing","document_only").contains(s(t,"originStatus")))return false;
        if ("manual".equals(f.get("origin"))&&!"manual".equals(t.get("originSource")))return false;
        for (String key:List.of("accountId","connectionId","categoryId","kind","product","billId")) {
            String value=f.get(key);
            if (value!=null && !value.isBlank() && !value.equals(s(t,key))) return false;
        }
        if ("true".equals(f.get("review")) && !Boolean.TRUE.equals(t.get("review"))) return false;
        if ("true".equals(f.get("ignored")) && !Boolean.TRUE.equals(t.get("ignored"))) return false;
        if ("false".equals(f.get("ignored")) && Boolean.TRUE.equals(t.get("ignored"))) return false;
        String d=s(t,"date"),from=f.get("from"),to=f.get("to");
        return (from==null||from.isBlank()||d.compareTo(from)>=0)&&(to==null||to.isBlank()||d.compareTo(to)<=0);
    }

    private List<Map<String,Object>> entries() {
        var result=new ArrayList<Map<String,Object>>();
        var rules=rules();
        for (var r:jdbc.queryForList("""
            SELECT t.*, a.type AS account_type,a.subtype AS account_subtype,COALESCE(a.display_name,a.name) AS account_name,
              c.display_name AS institution,cat.name AS category_name
            FROM transactions t JOIN accounts a ON a.id=t.account_id
            LEFT JOIN connections c ON c.id=t.connection_id LEFT JOIN categories cat ON cat.id=t.category_id
            ORDER BY t.date DESC,t.id DESC
            """)) {
            var raw=json(r.get("raw_payload")); var meta=raw.path("creditCardMetadata");
            String category=first(r.get("category_name"),r.get("pluggy_category")),categoryId=s(r,"category_id"),override=s(r,"financial_kind_override");
            String source=s(r,"categorization_source");
            if (!"manual".equals(source) && override.isBlank()) {
                for (var rule:rules) if (FinancePolicy.normalize(s(r,"description")).contains(FinancePolicy.normalize(s(rule,"pattern")))) {
                    if (rule.get("categoryId")!=null) { categoryId=s(rule,"categoryId");category=s(rule,"categoryName"); }
                    override=s(rule,"kind");source="rule";break;
                }
            }
            BigDecimal amount=n(r,"amount");
            String provider=first(text(meta,"type"),text(raw,"providerCode"),r.get("provider_code"));
            var classification=FinancePolicy.classify(s(r,"account_type"),s(r,"description"),first(r.get("pluggy_category"),category),provider,amount,override);
            String kind=classification.kind();
            var identity=TransactionDetails.identity(raw,s(r,"description"),s(r,"account_type"),amount,s(r,"counterparty_override"),kind);
            var timing=TransactionDetails.timing(raw);
            BigDecimal display=kind.equals("EXPENSE")?amount.abs().negate():Set.of("INCOME","REFUND").contains(kind)?amount.abs():amount;
            result.add(obj("id",r.get("id"),"accountId",r.get("account_id"),"connectionId",r.get("connection_id"),"institution",r.get("institution"),
                "accountName",r.get("account_name"),"product","CREDIT_CARD".equals(r.get("account_subtype"))?"CARD":"BANK","date",r.get("date"),
                "description",first(r.get("description"),"Sem descrição"),"counterparty",identity.name(),"counterpartyOverride",r.get("counterparty_override"),
                "originStatus",identity.status(),"originSource",identity.source(),"counterpartyDocumentPresent",identity.documentPresent(),
                "occurredAt",timing.timestamp(),"occurredOn",timing.localDate(),"localTime",timing.localTime(),"timeSource",timing.source(),
                "amount",amount,"displayAmount",display,"currency",r.get("currency_code"),
                "categoryId",categoryId,"categoryName",classification.review()&&"BANK".equals(r.get("account_type"))&&FinancePolicy.normalize(category).equals("credit card payment")?"Categoria a revisar":FinancePolicy.label(category),"kind",kind,"reason",classification.reason(),
                "review",classification.review(),"source",source,"notes",r.get("notes"),"ignored",n(r,"ignored").signum()!=0,
                "status",first(r.get("status"),"POSTED"),"billId",text(meta,"billId"),"installment",meta.path("installmentNumber").asInt(0),
                "installments",meta.path("totalInstallments").asInt(0),"purchaseDate",date(meta,"purchaseDate"),
                "merchant",first(identity.name(),r.get("description"))));
        }
        result.sort(Comparator.<Map<String,Object>,String>comparing(t -> s(t,"date")).thenComparing(t -> s(t,"localTime")).reversed());
        return result;
    }

    public List<Map<String,Object>> categories() {
        return jdbc.queryForList("SELECT id,name,kind,color,system,archived FROM categories ORDER BY name").stream().map(r ->
            obj("id",r.get("id"),"name",FinancePolicy.label(s(r,"name")),"kind",r.get("kind"),"color",r.get("color"),"system",n(r,"system").signum()!=0,"archived",n(r,"archived").signum()!=0)).toList();
    }
    public List<Map<String,Object>> rules() {
        return jdbc.queryForList("SELECT r.*,c.name AS category_name FROM classification_rules r LEFT JOIN categories c ON c.id=r.category_id ORDER BY r.created_at DESC,r.id").stream()
            .map(r -> obj("id",r.get("id"),"pattern",r.get("pattern"),"categoryId",r.get("category_id"),"categoryName",FinancePolicy.label(s(r,"category_name")),"kind",r.get("financial_kind"))).toList();
    }

    @Transactional
    public void editTransaction(String id,Map<String,Object> body) {
        requireRow("transactions",id);
        if (body.containsKey("categoryId")) {
            String category=nullable(body.get("categoryId"));if(category!=null)requireRow("categories",category);
            jdbc.update("UPDATE transactions SET category_id=?,categorization_source='manual',updated_at=CURRENT_TIMESTAMP WHERE id=?",category,id);
        }
        if (body.containsKey("kind")) {
            String kind=nullable(body.get("kind"));if(kind!=null&&!FinancePolicy.KINDS.contains(kind))throw new IllegalArgumentException("Tipo de movimento inválido.");
            jdbc.update("UPDATE transactions SET financial_kind_override=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",kind,id);
        }
        if(body.containsKey("notes"))jdbc.update("UPDATE transactions SET notes=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",limited(body.get("notes"),2000),id);
        if(body.containsKey("counterparty"))jdbc.update("UPDATE transactions SET counterparty_override=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",nullable(limited(body.get("counterparty"),160)),id);
        if(body.containsKey("ignored"))jdbc.update("UPDATE transactions SET ignored=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",Boolean.TRUE.equals(body.get("ignored")),id);
    }

    @Transactional
    public Map<String,Object> batch(Map<String,Object> body) {
        if(!(body.get("ids") instanceof List<?> ids)||ids.isEmpty()||ids.size()>500)throw new IllegalArgumentException("Selecione entre 1 e 500 transações.");
        if(!body.containsKey("kind")&&!body.containsKey("categoryId")&&!body.containsKey("ignored"))throw new IllegalArgumentException("Selecione uma alteração para o lote.");
        for(Object id:ids)editTransaction(id.toString(),body);
        return obj("updated",ids.size());
    }

    @Transactional
    public Map<String,Object> saveCategory(String id,Map<String,Object> body) {
        String name=limited(body.get("name"),100);if(name.isBlank())throw new IllegalArgumentException("Informe o nome da categoria.");
        String kind=first(body.get("kind"),"expense");if(!Set.of("expense","income","transfer").contains(kind))throw new IllegalArgumentException("Tipo de categoria inválido.");
        String color=first(body.get("color"),"#8d9f80");if(!color.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Cor inválida.");
        if(id==null) { id=UUID.randomUUID().toString();jdbc.update("INSERT INTO categories(id,name,kind,color,system) VALUES (?,?,?,?,0)",id,name,kind,color); }
        else { requireRow("categories",id);if(jdbc.queryForObject("SELECT system FROM categories WHERE id=?",Integer.class,id)==1)throw new IllegalArgumentException("Crie uma categoria pessoal para substituir a categoria do provedor.");
            jdbc.update("UPDATE categories SET name=?,kind=?,color=?,archived=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",name,kind,color,Boolean.TRUE.equals(body.get("archived")),id); }
        return obj("id",id);
    }

    @Transactional
    public Map<String,Object> saveRule(Map<String,Object> body) {
        String pattern=limited(body.get("pattern"),120);if(pattern.length()<3)throw new IllegalArgumentException("A regra precisa de pelo menos 3 caracteres.");
        String cat=nullable(body.get("categoryId")),kind=nullable(body.get("kind"));
        if(cat!=null)requireRow("categories",cat);
        if(kind!=null&&!FinancePolicy.KINDS.contains(kind))throw new IllegalArgumentException("Tipo de movimento inválido.");
        if(cat==null&&kind==null)throw new IllegalArgumentException("Defina uma categoria ou tipo de movimento.");
        String id=UUID.randomUUID().toString();jdbc.update("INSERT INTO classification_rules(id,pattern,category_id,financial_kind) VALUES (?,?,?,?)",id,pattern,cat,kind);
        return obj("id",id);
    }
    public void deleteRule(String id) { if(jdbc.update("DELETE FROM classification_rules WHERE id=?",id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND); }

    @Transactional
    public void editAccount(String id,Map<String,Object> body) {
        requireRow("accounts",id);
        if(body.containsKey("name"))jdbc.update("UPDATE accounts SET display_name=? WHERE id=?",nullable(limited(body.get("name"),100)),id);
        if(body.containsKey("paymentAccountId")) {
            String payment=nullable(body.get("paymentAccountId"));
            if(payment!=null) {
                requireRow("accounts",payment);
                if(!"BANK".equals(jdbc.queryForObject("SELECT type FROM accounts WHERE id=?",String.class,payment)))throw new IllegalArgumentException("Escolha uma conta corrente para pagar o cartão.");
            }
            if(!"CREDIT_CARD".equals(jdbc.queryForObject("SELECT subtype FROM accounts WHERE id=?",String.class,id)))throw new IllegalArgumentException("O vínculo de pagamento é exclusivo de cartões.");
            jdbc.update("UPDATE accounts SET payment_account_id=? WHERE id=?",payment,id);
        }
    }

    private List<Map<String,Object>> positions() {
        return jdbc.queryForList("SELECT i.*,c.display_name AS institution FROM investments i LEFT JOIN connections c ON c.id=i.connection_id ORDER BY i.name").stream().map(r -> {
            var raw=json(r.get("raw_payload"));String status=text(raw,"status");
            BigDecimal gross=dec(raw,"amount"),net=dec(raw,"balance"),quantity=dec(raw,"quantity"),unit=dec(raw,"value");
            boolean active=!Set.of("TOTAL_WITHDRAWAL","REDEEMED","CLOSED").contains(status==null?"":status)
                && ((gross!=null&&gross.signum()>0)||(net!=null&&net.signum()>0)||(quantity!=null&&quantity.signum()>0));
            return obj("id",r.get("id"),"name",r.get("name"),"institution",r.get("institution"),"connectionId",r.get("connection_id"),
                "type",r.get("type"),"subtype",r.get("subtype"),"active",active,"status",status,"grossAmount",gross,"netBalance",net,
                "unitValue",unit,"quantity",quantity,"originalAmount",dec(raw,"amountOriginal"),"reportedProfit",dec(raw,"amountProfit"),
                "taxes",dec(raw,"taxes"),"iof",dec(raw,"taxes2"),"currency",r.get("currency_code"),"date",date(raw,"date"),
                "dueDate",date(raw,"dueDate"),"purchaseDate",date(raw,"purchaseDate"),"rate",dec(raw,"rate"),"rateType",text(raw,"rateType"),
                "issuer",text(raw,"issuer"),"taxExempt",raw.path("taxExempt").isBoolean()?raw.path("taxExempt").asBoolean():null);
        }).toList();
    }
    private List<Map<String,Object>> investmentMovements() {
        return jdbc.queryForList("SELECT m.*,i.name AS investment_name,i.currency_code AS currency,c.display_name AS institution FROM investment_movements m JOIN investments i ON i.id=m.investment_id LEFT JOIN connections c ON c.id=i.connection_id ORDER BY m.date DESC LIMIT 500").stream()
            .map(r -> obj("id",r.get("id"),"investmentId",r.get("investment_id"),"investment",r.get("investment_name"),"institution",r.get("institution"),"currency",r.get("currency"),"date",r.get("date"),"type",r.get("type"),"amount",decimal(r.get("amount")),"netAmount",decimal(r.get("net_amount")),"description",r.get("description"))).toList();
    }
    private List<Map<String,Object>> investmentHistory() {
        return jdbc.queryForList("SELECT snapshot_date AS date,SUM(gross_amount) AS grossAmount,SUM(net_balance) AS netBalance,COUNT(*) AS positions FROM investment_snapshots WHERE currency_code='BRL' GROUP BY snapshot_date ORDER BY snapshot_date DESC LIMIT 366").reversed();
    }
    private List<Map<String,Object>> bills() {
        return jdbc.queryForList("SELECT b.*,a.name AS card_name,c.display_name AS institution FROM card_bills b JOIN accounts a ON a.id=b.account_id LEFT JOIN connections c ON c.id=a.connection_id ORDER BY b.due_date DESC").stream().map(r -> {
            var raw=json(r.get("raw_payload"));BigDecimal paid=BigDecimal.ZERO;
            boolean known=raw.path("payments").isArray()&&!raw.path("payments").isEmpty();
            for(var payment:raw.path("payments"))paid=paid.add(Optional.ofNullable(dec(payment,"amount")).orElse(BigDecimal.ZERO));
            var amount=decimal(r.get("amount"));String status=!known?"UNKNOWN":amount!=null&&paid.compareTo(amount)>=0?"PAID":"PARTIAL";
            return obj("id",r.get("id"),"accountId",r.get("account_id"),"institution",r.get("institution"),"cardName",r.get("card_name"),
                "dueDate",r.get("due_date"),"closingDate",r.get("closing_date"),"amount",amount,"currency",r.get("currency_code"),"paidAmount",known?paid:null,"status",status);
        }).toList();
    }

    private List<Map<String,Object>> recurrences(List<Map<String,Object>> all,YearMonth selected) {
        var groups=booked(all).stream().filter(t -> "EXPENSE".equals(t.get("kind")) && s(t,"date").compareTo(selected.minusMonths(5).atDay(1).toString())>=0)
            .collect(Collectors.groupingBy(t -> FinancePolicy.normalize(s(t,"merchant")).replaceAll("\\d+","").replaceAll("[^a-z ]","").trim()+"|"+s(t,"accountId")));
        var result=new ArrayList<Map<String,Object>>();
        for(var group:groups.entrySet()) {
            var ts=group.getValue();
            long months=ts.stream().map(t -> s(t,"date").substring(0,7)).distinct().count();
            if(months<3||ts.size()>months*2)continue;
            BigDecimal min=ts.stream().map(t -> n(t,"amount").abs()).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal max=ts.stream().map(t -> n(t,"amount").abs()).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if(min.signum()==0||max.compareTo(min.multiply(new BigDecimal("1.20")))>0)continue;
            var first=ts.getFirst();
            if(s(first,"date").compareTo(selected.minusMonths(1).atDay(1).toString())<0)continue;
            result.add(obj("id",UUID.nameUUIDFromBytes(group.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),"accountId",first.get("accountId"),"description",s(first,"merchant"),"institution",first.get("institution"),"amount",n(first,"amount").abs(),"months",months,"lastDate",first.get("date")));
        }
        return result.stream().sorted((a,b)->n(b,"amount").compareTo(n(a,"amount"))).limit(12).toList();
    }

    private List<Map<String,Object>> forecast(List<Map<String,Object>> all,YearMonth selected) {
        var latest=new LinkedHashMap<String,Map<String,Object>>();
        for(var t:booked(all)) {
            int total=n(t,"installments").intValue(),number=n(t,"installment").intValue();
            if(!"CARD".equals(t.get("product"))||!"EXPENSE".equals(t.get("kind"))||total<=number||number<1||total>60||Boolean.TRUE.equals(t.get("ignored"))||s(t,"date").isBlank())continue;
            String key=s(t,"accountId")+"|"+s(t,"purchaseDate")+"|"+FinancePolicy.normalize(s(t,"merchant")).replaceAll("\\d+","")+"|"+n(t,"amount").abs()+"|"+total;
            var previous=latest.get(key);if(previous==null||n(previous,"installment").intValue()<number)latest.put(key,t);
        }
        var result=new ArrayList<Map<String,Object>>();
        for(int i=1;i<=6;i++) {
            YearMonth target=selected.plusMonths(i);BigDecimal amount=BigDecimal.ZERO;int count=0;
            for(var t:latest.values()) {
                var base=YearMonth.parse(s(t,"date").substring(0,7));long distance=java.time.temporal.ChronoUnit.MONTHS.between(base,target);
                if(distance>0&&distance<=n(t,"installments").intValue()-n(t,"installment").intValue()){amount=amount.add(n(t,"amount").abs());count++;}
            }
            result.add(obj("month",target.toString(),"amount",amount,"installments",count));
        }
        return result;
    }

    private List<Map<String,Object>> booked(List<Map<String,Object>> rows) { String today=LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString();return rows.stream().filter(t -> !Boolean.TRUE.equals(t.get("ignored"))&&!"PENDING".equals(t.get("status"))&&!s(t,"date").isBlank()&&s(t,"date").compareTo(today)<=0&&"BRL".equals(t.get("currency"))).toList(); }
    private BigDecimal income(List<Map<String,Object>> rows){return rows.stream().filter(t -> "INCOME".equals(t.get("kind"))).map(t -> n(t,"amount").abs()).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private BigDecimal expenses(List<Map<String,Object>> rows){return rows.stream().map(t -> FinancePolicy.spending(s(t,"kind"),n(t,"amount"))).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private BigDecimal cash(List<Map<String,Object>> rows,boolean incoming){return rows.stream().filter(t -> "BANK".equals(t.get("product"))&&(incoming?n(t,"amount").signum()>0:n(t,"amount").signum()<0)).map(t -> n(t,"amount").abs()).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private BigDecimal sum(List<Map<String,Object>> rows,String key){return rows.stream().map(t -> n(t,key)).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private void requireRow(String table,String id){if(jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE id=?",Integer.class,id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro não encontrado.");}
    private static int positive(String v,int min,int max){int n=Integer.parseInt(v);if(n<min||n>max)throw new IllegalArgumentException("Paginação inválida.");return n;}
    private static YearMonth parseMonth(String month){return month==null||month.isBlank()?YearMonth.now(ZoneId.of("America/Sao_Paulo")):YearMonth.parse(month);}
    private JsonNode json(Object value){try{return mapper.readTree(value==null?"{}":value.toString());}catch(Exception e){return mapper.createObjectNode();}}
    private static String text(JsonNode n,String key){var v=n.path(key);return v.isNull()||v.isMissingNode()?null:v.asText();}
    private static String date(JsonNode n,String key){String v=text(n,key);return v==null||v.length()<10?null:v.substring(0,10);}
    private static BigDecimal dec(JsonNode n,String key){var v=n.path(key);try{return v.isNumber()||v.isTextual()?new BigDecimal(v.asText()):null;}catch(Exception e){return null;}}
    private static BigDecimal decimal(Object v){try{return v==null?null:new BigDecimal(v.toString());}catch(Exception e){return null;}}
    private static BigDecimal n(Map<String,?> m,String key){return Optional.ofNullable(decimal(m.get(key))).orElse(BigDecimal.ZERO);}
    private static String s(Map<String,?> m,String key){return m.get(key)==null?"":m.get(key).toString();}
    private static String nullable(Object v){return v==null||v.toString().isBlank()?null:v.toString();}
    private static String limited(Object v,int max){String s=v==null?"":v.toString().trim();if(s.length()>max)throw new IllegalArgumentException("Texto maior que o permitido.");return s;}
    private static String first(Object... values){for(Object v:values)if(v!=null&&!v.toString().isBlank())return v.toString();return null;}
    private static Map<String,Object> obj(Object... values){var result=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)result.put(values[i].toString(),values[i+1]);return result;}
}

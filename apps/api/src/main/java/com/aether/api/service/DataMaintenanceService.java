package com.aether.api.service;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DataMaintenanceService {
    public static final List<String> TABLES=List.of("sync_events","sync_jobs","card_bills","investment_movements","investment_snapshots","wealth_snapshots","visual_preferences","classification_rules","transactions","investments","accounts","connections","institutions","categories","app_settings");
    private final JdbcTemplate jdbc;
    private final WorkspaceService workspace;
    private final PluggyCredentialsService credentials;
    private final String backupOverride;
    private final BackupCrypto crypto;
    public DataMaintenanceService(JdbcTemplate jdbc, WorkspaceService workspace, PluggyCredentialsService credentials, BackupCrypto crypto, @Value("${aether.backup.directory:}") String backupOverride) {
        this.jdbc=jdbc;this.workspace=workspace;this.credentials=credentials;this.backupOverride=backupOverride;this.crypto=crypto;
    }
    public Map<String,Object> info() {
        return Map.of("backupDirectory",backupDirectory().toString(),"backupIncludesCredentials",false,"encryptedBackups",true,"backupExtension",".aetherbackup");
    }
    private Path backupDirectory() {
        if(!backupOverride.isBlank())return Path.of(backupOverride).toAbsolutePath();
        String url=jdbc.execute((ConnectionCallback<String>) c->c.getMetaData().getURL());
        String path=url.substring("jdbc:sqlite:".length());
        if(path.startsWith("file:")||path.equals(":memory:"))throw new IllegalStateException("Configure uma pasta de backup para este perfil.");
        return Path.of(path).toAbsolutePath().getParent().resolve("backups");
    }
    public byte[] exportBackup(String password) {
        crypto.validatePassword(password);
        byte[] plaintext=workspace.backup();
        try{return crypto.encrypt(plaintext,password);}finally{Arrays.fill(plaintext,(byte)0);}
    }
    private String safetyBackup(String reason,String password) {
        try {
            Path directory=backupDirectory();Files.createDirectories(directory);
            Path file=directory.resolve("aether-antes-"+reason+"-"+java.time.LocalDateTime.now().toString().replace(':','-')+"-"+UUID.randomUUID()+".aetherbackup");
            Files.write(file,exportBackup(password),StandardOpenOption.CREATE_NEW);
            return file.toString();
        }catch(java.io.IOException e){throw new IllegalArgumentException("Não foi possível criar a cópia de segurança. Seus dados não foram alterados.");}
    }
    public Map<String,Object> reset(String confirmation,String password) {
        if(!"APAGAR".equals(confirmation))throw new IllegalArgumentException("Digite APAGAR para confirmar a remoção dos dados locais.");
        crypto.validatePassword(password);
        String backup=safetyBackup("apagar",password);
        replaceData(null);
        credentials.forgetRuntime();
        return Map.of("ok",true,"safetyBackup",backup);
    }
    public Map<String,Object> restore(byte[] archive,String confirmation,String password) {
        if(!"RESTAURAR".equals(confirmation))throw new IllegalArgumentException("Digite RESTAURAR para confirmar a substituição dos dados locais.");
        crypto.validatePassword(password);
        byte[] content=crypto.encrypted(archive)?crypto.decrypt(archive,password):archive;
        if(content.length<16||content.length>BackupCrypto.MAX_PLAINTEXT||!new String(content,0,16,java.nio.charset.StandardCharsets.US_ASCII).equals("SQLite format 3\0"))
            throw new IllegalArgumentException("Escolha um backup SQLite do Aether de até 100 MB.");
        Path temporary=null;
        try {
            temporary=Files.createTempFile("aether-restore-",".sqlite");Files.write(temporary,content);
            validateAndMigrate(temporary);
            String backup=safetyBackup("restaurar",password);
            replaceData(temporary);
            return Map.of("ok",true,"safetyBackup",backup);
        }catch(java.io.IOException e){throw new IllegalArgumentException("Não foi possível ler o backup. Seus dados não foram alterados.");}
        finally{if(content!=archive)Arrays.fill(content,(byte)0);if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(java.io.IOException ignored){}}
    }
    private void validateAndMigrate(Path file) {
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+file.toString())) {
            try(var result=c.createStatement().executeQuery("PRAGMA integrity_check")){if(!result.next()||!"ok".equals(result.getString(1)))throw new IllegalArgumentException();}
            try(var result=c.createStatement().executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('transactions','accounts','connections','flyway_schema_history')")){if(!result.next()||result.getInt(1)!=4)throw new IllegalArgumentException();}
        }catch(Exception e){throw new IllegalArgumentException("Este arquivo não é um backup íntegro do Aether.");}
        try {
            Flyway.configure().dataSource("jdbc:sqlite:"+file.toString(),null,null).locations("classpath:db/migration").load().migrate();
            try(var c=DriverManager.getConnection("jdbc:sqlite:"+file.toString())) {
                for(String table:TABLES) {
                    try(var s=c.prepareStatement("SELECT type FROM sqlite_master WHERE name=?")){s.setString(1,table);try(var r=s.executeQuery()){if(!r.next()||!"table".equals(r.getString(1)))throw new IllegalArgumentException();}}
                    var sourceColumns=new HashSet<String>();
                    try(var r=c.createStatement().executeQuery("PRAGMA table_info("+table+")")){while(r.next())sourceColumns.add(r.getString("name"));}
                    var targetColumns=jdbc.queryForList("PRAGMA table_info("+table+")").stream().map(r->r.get("name").toString()).toList();
                    if(!sourceColumns.containsAll(targetColumns))throw new IllegalArgumentException();
                }
                try(var r=c.createStatement().executeQuery("PRAGMA foreign_key_check")){if(r.next())throw new IllegalArgumentException();}
            }
        }catch(Exception e){throw new IllegalArgumentException("O backup é incompatível ou contém vínculos inválidos. Use um backup exportado por uma versão compatível do Aether.");}
    }
    private void replaceData(Path source) {
        jdbc.execute((ConnectionCallback<Void>) c->{
            boolean oldAuto=c.getAutoCommit();
            if(source!=null)try(var attach=c.prepareStatement("ATTACH DATABASE ? AS restore_source")){attach.setString(1,source.toString());attach.execute();}
            try {
                c.setAutoCommit(false);
                try(var statement=c.createStatement()) {
                    statement.execute("PRAGMA defer_foreign_keys=ON");
                    for(String table:TABLES)statement.executeUpdate("DELETE FROM "+table);
                    if(source!=null)for(String table:TABLES.reversed()){
                        var columns=new ArrayList<String>();
                        try(var r=statement.executeQuery("PRAGMA main.table_info("+table+")")){while(r.next())columns.add("\""+r.getString("name")+"\"");}
                        String fields=String.join(",",columns);
                        statement.executeUpdate("INSERT INTO main."+table+" ("+fields+") SELECT "+fields+" FROM restore_source."+table);
                    }
                    else statement.executeUpdate("INSERT INTO app_settings(key,value) VALUES ('pluggy_credentials_disabled','true')");
                    try(var r=statement.executeQuery("PRAGMA main.foreign_key_check")){if(r.next())throw new SQLException("Invalid foreign key in restored data");}
                }
                c.commit();
            } catch(Exception e) { c.rollback();throw new IllegalArgumentException("Não foi possível concluir a operação. Os dados anteriores foram preservados."); }
            finally { c.setAutoCommit(oldAuto);if(source!=null)try(var detach=c.createStatement()){detach.execute("DETACH DATABASE restore_source");} }
            return null;
        });
    }
}

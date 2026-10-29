package com.aether.api.service;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties="spring.datasource.url=jdbc:sqlite:file:maintenance-test?mode=memory&cache=shared")
class DataMaintenanceServiceTests {
    static final String PASSWORD="example-test-passphrase-2026";
    @Autowired BackupCrypto crypto;
    static final Path BACKUPS;
    static {try{BACKUPS=Files.createTempDirectory("aether-maintenance-test-");}catch(Exception e){throw new RuntimeException(e);}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("aether.backup.directory",()->BACKUPS.toString());}
    @Autowired DataMaintenanceService maintenance;
    @Autowired WorkspaceService workspace;
    @Autowired PluggyCredentialsService credentials;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void setup(){
        jdbc.execute("DROP TRIGGER IF EXISTS fail_restore");
        for(String table:DataMaintenanceService.TABLES)jdbc.update("DELETE FROM "+table);
        credentials.forgetRuntime();
        jdbc.update("INSERT INTO connections(id,display_name,status) VALUES('bank','Banco','UPDATED')");
        jdbc.update("INSERT INTO categories(id,name,kind) VALUES('cat','Mercado','expense')");
        jdbc.update("INSERT INTO accounts(id,connection_id,type,name,balance) VALUES('account','bank','BANK','Conta',100)");
        jdbc.update("INSERT INTO transactions(id,account_id,connection_id,category_id,date,description,amount,status) VALUES('entry','account','bank','cat','2026-10-01','Compra',-10,'POSTED')");
        jdbc.update("INSERT INTO visual_preferences(scope,target_id,alias) VALUES('institution','bank','Meu banco')");
    }
    int count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class);}
    @Test void exportsResetsAndRestoresAnEntireProfileWithoutCredentials()throws Exception{
        credentials.update(new com.aether.api.dto.SyncDtos.PluggyCredentialsRequest("dummy-client","dummy-secret"));
        byte[] backup=maintenance.exportBackup(PASSWORD);
        assertThat(crypto.encrypted(backup)).isTrue();
        var reset=maintenance.reset("APAGAR",PASSWORD);
        var safety=Files.readAllBytes(Path.of(reset.get("safetyBackup").toString()));
        assertThat(crypto.encrypted(safety)).isTrue();
        assertThat(new String(crypto.decrypt(safety,PASSWORD),0,15,java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("SQLite format 3");
        for(String table:DataMaintenanceService.TABLES)if(!table.equals("app_settings"))assertThat(count(table)).isZero();
        assertThat(workspace.workspace("2026-10").get("firstRun")).isEqualTo(true);
        assertThatThrownBy(()->credentials.resolve()).isInstanceOf(PluggyException.class);
        maintenance.restore(backup,"RESTAURAR",PASSWORD);
        assertThat(count("transactions")).isEqualTo(1);assertThat(count("accounts")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT alias FROM visual_preferences",String.class)).isEqualTo("Meu banco");
        assertThat(workspace.workspace("2026-10").get("firstRun")).isEqualTo(false);
        assertThatThrownBy(()->credentials.resolve()).isInstanceOf(PluggyException.class);
    }
    @Test void requiresConfirmationAndRejectsInvalidFilesWithoutChangingData(){
        assertThatThrownBy(()->maintenance.reset("sim",PASSWORD)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->maintenance.restore(workspace.backup(),"sim",PASSWORD)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->maintenance.restore("invalid".getBytes(),"RESTAURAR",PASSWORD)).isInstanceOf(IllegalArgumentException.class);
        assertThat(count("transactions")).isEqualTo(1);
    }
    @Test void rejectsForeignKeyDamageAndMissingColumnsBeforeReplacingData()throws Exception{
        for(boolean missingColumn:List.of(false,true)){
            Path file=Files.createTempFile("aether-bad-backup-",".sqlite");Files.write(file,workspace.backup());
            try(var c=DriverManager.getConnection("jdbc:sqlite:"+file)){
                c.createStatement().execute(missingColumn?"ALTER TABLE transactions RENAME COLUMN amount TO bad_amount":"UPDATE transactions SET account_id='missing'");
            }
            assertThatThrownBy(()->maintenance.restore(Files.readAllBytes(file),"RESTAURAR",PASSWORD)).isInstanceOf(IllegalArgumentException.class);
            assertThat(count("transactions")).isEqualTo(1);Files.delete(file);
        }
    }
    @Test void rollsBackTheWholeReplacementIfAnyInsertFails(){
        byte[] backup=workspace.backup();
        jdbc.update("UPDATE transactions SET description='Current history'");
        jdbc.execute("CREATE TRIGGER fail_restore BEFORE INSERT ON transactions BEGIN SELECT RAISE(ABORT,'test failure');END");
        assertThatThrownBy(()->maintenance.restore(backup,"RESTAURAR",PASSWORD)).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT description FROM transactions",String.class)).isEqualTo("Current history");
        assertThat(count("accounts")).isEqualTo(1);
    }
    @Test void refusesResetWhenTheSafetyBackupCannotBeWritten()throws Exception{
        Path blocked=Files.createTempFile("aether-backup-blocked-",".tmp");
        var guarded=new DataMaintenanceService(jdbc,workspace,credentials,crypto,blocked.toString());
        assertThatThrownBy(()->guarded.reset("APAGAR",PASSWORD)).isInstanceOf(IllegalArgumentException.class);
        assertThat(count("transactions")).isEqualTo(1);Files.delete(blocked);
    }
    @Test void refusesWrongPasswordAndTamperedArchiveWithoutChangingData(){
        byte[] archive=maintenance.exportBackup(PASSWORD);
        assertThatThrownBy(()->maintenance.restore(archive,"RESTAURAR","another-wrong-password")).isInstanceOf(IllegalArgumentException.class);
        archive[archive.length-1]^=1;
        assertThatThrownBy(()->maintenance.restore(archive,"RESTAURAR",PASSWORD)).isInstanceOf(IllegalArgumentException.class);
        assertThat(count("transactions")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT description FROM transactions",String.class)).isEqualTo("Compra");
    }
    @Test void refusesUnprotectedSafetyCopiesAndWeakPasswords(){
        assertThatThrownBy(()->maintenance.reset("APAGAR",null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->maintenance.exportBackup("short")).isInstanceOf(IllegalArgumentException.class);
        assertThat(count("transactions")).isEqualTo(1);
    }
}

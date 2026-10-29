package com.aether.api.controller;

import com.aether.api.service.DataMaintenanceService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data")
public class DataMaintenanceController {
    private final DataMaintenanceService service;
    public DataMaintenanceController(DataMaintenanceService service){this.service=service;}
    @GetMapping public Map<String,Object> info(){return service.info();}
    @PostMapping("/export") public org.springframework.http.ResponseEntity<byte[]> export(@RequestBody Map<String,String> body){
        byte[] archive=service.exportBackup(body.get("password"));
        String day=java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();
        return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=\"aether-backup-"+day+".aetherbackup\"")
            .header("Cache-Control","no-store").contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM).body(archive);
    }
    @PostMapping("/reset") public Map<String,Object> reset(@RequestBody Map<String,String> body){return service.reset(body.get("confirmation"),body.get("password"));}
    @PostMapping(value="/restore",consumes="multipart/form-data") public Map<String,Object> restore(@RequestParam("file") MultipartFile file,@RequestParam("confirmation") String confirmation,@RequestParam("password") String password) throws java.io.IOException {
        if(file.getSize()>com.aether.api.service.BackupCrypto.MAX_ARCHIVE)throw new IllegalArgumentException("O backup deve ter até 100 MB.");
        return service.restore(file.getBytes(),confirmation,password);
    }
}

package com.aether.api.controller;

import com.aether.api.service.WorkspaceService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mvp")
public class WorkspaceController {
    private final WorkspaceService service;
    public WorkspaceController(WorkspaceService service){this.service=service;}
    @GetMapping("/workspace") public Map<String,Object> workspace(@RequestParam(required=false)String month){return service.workspace(month);}
    @GetMapping("/transactions") public Map<String,Object> transactions(@RequestParam Map<String,String> filters){return service.transactions(filters);}
    @PatchMapping("/transactions/{id}") public Map<String,Boolean> edit(@PathVariable String id,@RequestBody Map<String,Object> body){service.editTransaction(id,body);return Map.of("ok",true);}
    @PostMapping("/transactions/batch") public Map<String,Object> batch(@RequestBody Map<String,Object> body){return service.batch(body);}
    @PostMapping("/categories") public Map<String,Object> category(@RequestBody Map<String,Object> body){return service.saveCategory(null,body);}
    @PutMapping("/categories/{id}") public Map<String,Object> category(@PathVariable String id,@RequestBody Map<String,Object> body){return service.saveCategory(id,body);}
    @PostMapping("/rules") public Map<String,Object> rule(@RequestBody Map<String,Object> body){return service.saveRule(body);}
    @DeleteMapping("/rules/{id}") public Map<String,Boolean> deleteRule(@PathVariable String id){service.deleteRule(id);return Map.of("ok",true);}
    @PatchMapping("/accounts/{id}") public Map<String,Boolean> account(@PathVariable String id,@RequestBody Map<String,Object> body){service.editAccount(id,body);return Map.of("ok",true);}
    @PutMapping("/visual-preferences") public Map<String,Boolean> visual(@RequestBody Map<String,Object> body){service.saveVisualPreference(body);return Map.of("ok",true);}
    @GetMapping("/backup") public void obsoleteBackup(){
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE,"O backup agora exige senha. Use Dados e backup na versão atual do Aether.");
    }
}

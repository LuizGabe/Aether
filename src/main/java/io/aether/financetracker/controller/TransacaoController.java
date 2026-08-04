package io.aether.financetracker.controller;

import io.aether.financetracker.model.Transacao;
import io.aether.financetracker.service.TransacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    private final TransacaoService service;

    // Injeção de dependência via construtor
    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    // GET /api/transacoes
    @GetMapping
    public List<Transacao> listar() {
        return service.listarTodas();
    }

    // GET /api/transacoes/1
    @GetMapping("/{id}")
    public ResponseEntity<Transacao> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/transacoes
    @PostMapping
    public ResponseEntity<Transacao> criar(@RequestBody Transacao transacao) {
        Transacao criada = service.salvar(transacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    // PUT /api/transacoes/1
    @PutMapping("/{id}")
    public ResponseEntity<Transacao> atualizar(@PathVariable Long id, @RequestBody Transacao transacao) {
        return service.atualizar(id, transacao)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/transacoes/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (service.deletar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
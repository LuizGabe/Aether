package io.aether.financetracker.service;

import io.aether.financetracker.model.Transacao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TransacaoService {

    // Lista em memória simulando a tabela do banco
    private final List<Transacao> transacoes = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public TransacaoService() {
        // Dados de teste pré-carregados
        salvar(new Transacao(null, "Salário", new BigDecimal("5000.00"), "RECEITA"));
        salvar(new Transacao(null, "Aluguel", new BigDecimal("1500.00"), "DESPESA"));
    }

    public List<Transacao> listarTodas() {
        return transacoes;
    }

    public Optional<Transacao> buscarPorId(Long id) {
        return transacoes.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    public Transacao salvar(Transacao transacao) {
        if (transacao.getId() == null) {
            transacao.setId(idGenerator.getAndIncrement());
        }
        transacoes.add(transacao);
        return transacao;
    }

    public Optional<Transacao> atualizar(Long id, Transacao novaTransacao) {
        return buscarPorId(id).map(transacaoExistente -> {
            transacaoExistente.setDescricao(novaTransacao.getDescricao());
            transacaoExistente.setValor(novaTransacao.getValor());
            transacaoExistente.setTipo(novaTransacao.getTipo());
            return transacaoExistente;
        });
    }

    public boolean deletar(Long id) {
        return transacoes.removeIf(t -> t.getId().equals(id));
    }
}
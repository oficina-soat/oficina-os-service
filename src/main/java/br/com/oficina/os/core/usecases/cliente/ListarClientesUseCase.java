package br.com.oficina.os.core.usecases.cliente;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteSearchCriteria;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListarClientesUseCase {
    private final AtendimentoGateway gateway;

    public ListarClientesUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<List<ClienteRecord>> executar(Query query) {
        var criteria = new ClienteSearchCriteria(query.nome(), query.documento(), query.email());
        return CompletableFuture.completedFuture(gateway.listarClientes(criteria));
    }

    public record Query(String nome, String documento, String email) {
    }
}

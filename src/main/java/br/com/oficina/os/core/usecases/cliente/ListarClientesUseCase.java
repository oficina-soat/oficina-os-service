package br.com.oficina.os.core.usecases.cliente;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListarClientesUseCase {
    private final AtendimentoGateway gateway;

    public ListarClientesUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<List<ClienteRecord>> executar() {
        return CompletableFuture.completedFuture(gateway.listarClientes());
    }
}

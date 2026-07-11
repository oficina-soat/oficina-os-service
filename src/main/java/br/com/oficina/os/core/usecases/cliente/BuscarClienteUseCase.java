package br.com.oficina.os.core.usecases.cliente;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BuscarClienteUseCase {
    private final AtendimentoGateway gateway;

    public BuscarClienteUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<ClienteRecord> executar(UUID clienteId) {
        return CompletableFuture.completedFuture(gateway.buscarCliente(clienteId));
    }
}

package br.com.oficina.os.core.usecases.veiculo;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ListarVeiculosDoClienteUseCase {
    private final AtendimentoGateway gateway;

    public ListarVeiculosDoClienteUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<List<VeiculoRecord>> executar(UUID clienteId) {
        return CompletableFuture.completedFuture(gateway.listarVeiculosDoCliente(clienteId));
    }
}

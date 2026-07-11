package br.com.oficina.os.core.usecases.veiculo;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BuscarVeiculoUseCase {
    private final AtendimentoGateway gateway;

    public BuscarVeiculoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<VeiculoRecord> executar(UUID veiculoId) {
        return CompletableFuture.completedFuture(gateway.buscarVeiculo(veiculoId));
    }
}

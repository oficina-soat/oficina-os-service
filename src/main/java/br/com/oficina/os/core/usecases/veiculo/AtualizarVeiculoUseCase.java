package br.com.oficina.os.core.usecases.veiculo;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AtualizarVeiculoUseCase {
    private final AtendimentoGateway gateway;

    public AtualizarVeiculoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<VeiculoRecord> executar(Command command) {
        return CompletableFuture.completedFuture(gateway.atualizarVeiculo(
                command.veiculoId(),
                command.placa(),
                command.marca(),
                command.modelo(),
                command.ano()));
    }

    public record Command(
            UUID veiculoId,
            String placa,
            String marca,
            String modelo,
            int ano) {
    }
}

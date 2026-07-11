package br.com.oficina.os.core.usecases.veiculo;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CriarVeiculoUseCase {
    private final AtendimentoGateway gateway;

    public CriarVeiculoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<VeiculoRecord> executar(Command command) {
        return CompletableFuture.completedFuture(gateway.criarVeiculo(
                command.clienteId(),
                command.placa(),
                command.marca(),
                command.modelo(),
                command.ano()));
    }

    public record Command(
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            int ano) {
    }
}

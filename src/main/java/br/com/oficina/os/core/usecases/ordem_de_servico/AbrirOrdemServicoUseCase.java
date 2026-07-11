package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AbrirOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public AbrirOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<OrdemServicoRecord> executar(Command command) {
        return CompletableFuture.completedFuture(gateway.criarOrdemServico(
                command.clienteId(),
                command.veiculoId(),
                command.descricaoProblema()));
    }

    public record Command(
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema) {
    }
}

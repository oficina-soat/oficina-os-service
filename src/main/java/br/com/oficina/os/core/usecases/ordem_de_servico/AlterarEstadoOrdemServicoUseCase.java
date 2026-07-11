package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AlterarEstadoOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public AlterarEstadoOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<OrdemServicoRecord> executar(Command command) {
        return CompletableFuture.completedFuture(gateway.alterarEstado(
                command.ordemServicoId(),
                command.novoEstado(),
                command.motivo()));
    }

    public record Command(
            UUID ordemServicoId,
            TipoDeEstadoDaOrdemDeServico novoEstado,
            String motivo) {
    }
}

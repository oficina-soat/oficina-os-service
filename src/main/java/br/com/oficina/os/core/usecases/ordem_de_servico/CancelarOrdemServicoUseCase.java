package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OperacaoAssincronaRecord;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CancelarOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public CancelarOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<OperacaoAssincronaRecord> executar(Command command) {
        return CompletableFuture.completedFuture(gateway.cancelar(command.ordemServicoId(), command.motivo()));
    }

    public record Command(UUID ordemServicoId, String motivo) {
    }
}

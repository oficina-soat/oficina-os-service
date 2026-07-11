package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.HistoricoRecord;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConsultarHistoricoOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public ConsultarHistoricoOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<List<HistoricoRecord>> executar(UUID ordemServicoId) {
        return CompletableFuture.completedFuture(gateway.historico(ordemServicoId));
    }
}

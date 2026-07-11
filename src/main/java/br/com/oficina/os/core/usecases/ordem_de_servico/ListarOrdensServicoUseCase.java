package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListarOrdensServicoUseCase {
    private final AtendimentoGateway gateway;

    public ListarOrdensServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<List<OrdemServicoRecord>> executar(TipoDeEstadoDaOrdemDeServico estado) {
        return CompletableFuture.completedFuture(gateway.listarOrdensServico(estado));
    }
}

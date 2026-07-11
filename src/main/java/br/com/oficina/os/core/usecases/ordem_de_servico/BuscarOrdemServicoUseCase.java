package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import java.util.UUID;

public class BuscarOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public BuscarOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public OrdemServicoRecord executar(UUID ordemServicoId) {
        return gateway.buscarOrdemServico(ordemServicoId);
    }
}

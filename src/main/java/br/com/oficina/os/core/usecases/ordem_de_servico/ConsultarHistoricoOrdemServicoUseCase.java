package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.HistoricoRecord;
import java.util.List;
import java.util.UUID;

public class ConsultarHistoricoOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public ConsultarHistoricoOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public List<HistoricoRecord> executar(UUID ordemServicoId) {
        return gateway.historico(ordemServicoId);
    }
}

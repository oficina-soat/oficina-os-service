package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OperacaoAssincronaRecord;
import java.util.UUID;

public class CancelarOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public CancelarOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public OperacaoAssincronaRecord executar(UUID ordemServicoId, String motivo) {
        return gateway.cancelar(ordemServicoId, motivo);
    }
}

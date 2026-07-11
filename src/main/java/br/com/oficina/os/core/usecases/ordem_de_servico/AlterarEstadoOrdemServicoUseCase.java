package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import java.util.UUID;

public class AlterarEstadoOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public AlterarEstadoOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public OrdemServicoRecord executar(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        return gateway.alterarEstado(ordemServicoId, novoEstado, motivo);
    }
}

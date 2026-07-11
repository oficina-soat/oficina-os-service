package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import java.util.List;

public class ListarOrdensServicoUseCase {
    private final AtendimentoGateway gateway;

    public ListarOrdensServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public List<OrdemServicoRecord> executar(TipoDeEstadoDaOrdemDeServico estado) {
        return gateway.listarOrdensServico(estado);
    }
}

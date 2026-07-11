package br.com.oficina.os.core.usecases.ordem_de_servico;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import java.util.UUID;

public class AbrirOrdemServicoUseCase {
    private final AtendimentoGateway gateway;

    public AbrirOrdemServicoUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public OrdemServicoRecord executar(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        return gateway.criarOrdemServico(clienteId, veiculoId, descricaoProblema);
    }
}

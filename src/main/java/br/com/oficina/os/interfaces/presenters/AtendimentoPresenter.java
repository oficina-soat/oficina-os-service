package br.com.oficina.os.interfaces.presenters;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.HistoricoRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import br.com.oficina.os.interfaces.controllers.ClientesResource;
import br.com.oficina.os.interfaces.controllers.OrdensServicoResource;
import br.com.oficina.os.interfaces.controllers.VeiculosResource;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AtendimentoPresenter {

    public ClientesResource.ClienteResponse cliente(ClienteRecord cliente) {
        return new ClientesResource.ClienteResponse(
                cliente.clienteId(),
                cliente.nome(),
                cliente.documento(),
                cliente.telefone(),
                cliente.email(),
                cliente.criadoEm(),
                cliente.atualizadoEm());
    }

    public VeiculosResource.VeiculoResponse veiculo(VeiculoRecord veiculo) {
        return new VeiculosResource.VeiculoResponse(
                veiculo.veiculoId(),
                veiculo.clienteId(),
                veiculo.placa(),
                veiculo.marca(),
                veiculo.modelo(),
                veiculo.ano(),
                veiculo.criadoEm(),
                veiculo.atualizadoEm());
    }

    public OrdensServicoResource.OrdemServicoResponse ordemServico(OrdemServicoRecord ordem) {
        return new OrdensServicoResource.OrdemServicoResponse(
                ordem.ordemServicoId(),
                ordem.clienteId(),
                ordem.veiculoId(),
                ordem.descricaoProblema(),
                ordem.estado(),
                ordem.criadoEm(),
                ordem.atualizadoEm());
    }

    public OrdensServicoResource.HistoricoOrdemServicoResponse historico(HistoricoRecord historico) {
        return new OrdensServicoResource.HistoricoOrdemServicoResponse(
                historico.estado(),
                historico.dataDoEstado(),
                historico.motivo());
    }
}

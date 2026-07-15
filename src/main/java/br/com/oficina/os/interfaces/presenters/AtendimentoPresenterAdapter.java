package br.com.oficina.os.interfaces.presenters;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.HistoricoRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.VeiculoRecord;
import br.com.oficina.os.interfaces.presenters.view_model.ClienteViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.HistoricoOrdemServicoViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.OrdemServicoViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.VeiculoViewModel;
import br.com.oficina.os.core.entities.ordem_de_servico.AcaoPermitidaOrdemServico;

public class AtendimentoPresenterAdapter {

    public ClienteViewModel cliente(ClienteRecord cliente) {
        return new ClienteViewModel(
                cliente.clienteId(),
                cliente.nome(),
                cliente.documento(),
                cliente.telefone(),
                cliente.email(),
                cliente.criadoEm(),
                cliente.atualizadoEm());
    }

    public VeiculoViewModel veiculo(VeiculoRecord veiculo) {
        return new VeiculoViewModel(
                veiculo.veiculoId(),
                veiculo.clienteId(),
                veiculo.placa(),
                veiculo.marca(),
                veiculo.modelo(),
                veiculo.ano(),
                veiculo.criadoEm(),
                veiculo.atualizadoEm());
    }

    public OrdemServicoViewModel ordemServico(OrdemServicoRecord ordem) {
        return new OrdemServicoViewModel(
                ordem.ordemServicoId(),
                ordem.clienteId(),
                ordem.veiculoId(),
                ordem.descricaoProblema(),
                ordem.estado(),
                ordem.criadoEm(),
                ordem.atualizadoEm(),
                AcaoPermitidaOrdemServico.porEstado(ordem.estado()));
    }

    public HistoricoOrdemServicoViewModel historico(HistoricoRecord historico) {
        return new HistoricoOrdemServicoViewModel(
                historico.estado(),
                historico.dataDoEstado(),
                historico.motivo());
    }
}

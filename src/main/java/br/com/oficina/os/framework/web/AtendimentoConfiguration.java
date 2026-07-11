package br.com.oficina.os.framework.web;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.usecases.cliente.AtualizarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.BuscarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.CriarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.ListarClientesUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.AbrirOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.AlterarEstadoOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.BuscarOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.CancelarOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.ConsultarHistoricoOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.ListarOrdensServicoUseCase;
import br.com.oficina.os.core.usecases.outbox.PublicarEventosPendentesUseCase;
import br.com.oficina.os.core.usecases.saga.ConsumirEventoDaSagaUseCase;
import br.com.oficina.os.core.usecases.veiculo.AtualizarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.BuscarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.CriarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.ListarVeiculosDoClienteUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class AtendimentoConfiguration {
    @Produces
    CriarClienteUseCase criarClienteUseCase(AtendimentoGateway gateway) {
        return new CriarClienteUseCase(gateway);
    }

    @Produces
    ListarClientesUseCase listarClientesUseCase(AtendimentoGateway gateway) {
        return new ListarClientesUseCase(gateway);
    }

    @Produces
    BuscarClienteUseCase buscarClienteUseCase(AtendimentoGateway gateway) {
        return new BuscarClienteUseCase(gateway);
    }

    @Produces
    AtualizarClienteUseCase atualizarClienteUseCase(AtendimentoGateway gateway) {
        return new AtualizarClienteUseCase(gateway);
    }

    @Produces
    CriarVeiculoUseCase criarVeiculoUseCase(AtendimentoGateway gateway) {
        return new CriarVeiculoUseCase(gateway);
    }

    @Produces
    ListarVeiculosDoClienteUseCase listarVeiculosDoClienteUseCase(AtendimentoGateway gateway) {
        return new ListarVeiculosDoClienteUseCase(gateway);
    }

    @Produces
    BuscarVeiculoUseCase buscarVeiculoUseCase(AtendimentoGateway gateway) {
        return new BuscarVeiculoUseCase(gateway);
    }

    @Produces
    AtualizarVeiculoUseCase atualizarVeiculoUseCase(AtendimentoGateway gateway) {
        return new AtualizarVeiculoUseCase(gateway);
    }

    @Produces
    AbrirOrdemServicoUseCase abrirOrdemServicoUseCase(AtendimentoGateway gateway) {
        return new AbrirOrdemServicoUseCase(gateway);
    }

    @Produces
    ListarOrdensServicoUseCase listarOrdensServicoUseCase(AtendimentoGateway gateway) {
        return new ListarOrdensServicoUseCase(gateway);
    }

    @Produces
    BuscarOrdemServicoUseCase buscarOrdemServicoUseCase(AtendimentoGateway gateway) {
        return new BuscarOrdemServicoUseCase(gateway);
    }

    @Produces
    ConsultarHistoricoOrdemServicoUseCase consultarHistoricoOrdemServicoUseCase(AtendimentoGateway gateway) {
        return new ConsultarHistoricoOrdemServicoUseCase(gateway);
    }

    @Produces
    AlterarEstadoOrdemServicoUseCase alterarEstadoOrdemServicoUseCase(AtendimentoGateway gateway) {
        return new AlterarEstadoOrdemServicoUseCase(gateway);
    }

    @Produces
    CancelarOrdemServicoUseCase cancelarOrdemServicoUseCase(AtendimentoGateway gateway) {
        return new CancelarOrdemServicoUseCase(gateway);
    }

    @Produces
    ConsumirEventoDaSagaUseCase consumirEventoDaSagaUseCase(AtendimentoGateway gateway) {
        return new ConsumirEventoDaSagaUseCase(gateway);
    }

    @Produces
    PublicarEventosPendentesUseCase publicarEventosPendentesUseCase(AtendimentoGateway gateway) {
        return new PublicarEventosPendentesUseCase(gateway);
    }
}

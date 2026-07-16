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
import br.com.oficina.os.core.usecases.ordem_de_servico.IncluirServicoOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.IncluirPecaOrdemServicoUseCase;
import br.com.oficina.os.core.interfaces.gateway.CatalogoGateway;
import br.com.oficina.os.core.usecases.outbox.PublicarEventosPendentesUseCase;
import br.com.oficina.os.core.usecases.saga.ConsumirEventoDaSagaUseCase;
import br.com.oficina.os.core.usecases.veiculo.AtualizarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.BuscarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.CriarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.ListarVeiculosDoClienteUseCase;
import br.com.oficina.os.interfaces.controllers.ClientesController;
import br.com.oficina.os.interfaces.controllers.OrdensServicoController;
import br.com.oficina.os.interfaces.controllers.StatusController;
import br.com.oficina.os.interfaces.controllers.VeiculosController;
import br.com.oficina.os.interfaces.presenters.AtendimentoPresenterAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
    IncluirServicoOrdemServicoUseCase incluirServicoOrdemServicoUseCase(AtendimentoGateway gateway, CatalogoGateway catalogo) {
        return new IncluirServicoOrdemServicoUseCase(gateway, catalogo);
    }

    @Produces
    IncluirPecaOrdemServicoUseCase incluirPecaOrdemServicoUseCase(AtendimentoGateway gateway, CatalogoGateway catalogo) {
        return new IncluirPecaOrdemServicoUseCase(gateway, catalogo);
    }

    @Produces
    ConsumirEventoDaSagaUseCase consumirEventoDaSagaUseCase(AtendimentoGateway gateway) {
        return new ConsumirEventoDaSagaUseCase(gateway);
    }

    @Produces
    PublicarEventosPendentesUseCase publicarEventosPendentesUseCase(AtendimentoGateway gateway) {
        return new PublicarEventosPendentesUseCase(gateway);
    }

    @Produces
    ClientesController clientesController(
            CriarClienteUseCase criarCliente,
            ListarClientesUseCase listarClientes,
            BuscarClienteUseCase buscarCliente,
            AtualizarClienteUseCase atualizarCliente,
            CriarVeiculoUseCase criarVeiculo,
            ListarVeiculosDoClienteUseCase listarVeiculosDoCliente,
            AtendimentoPresenterAdapter presenter) {
        return new ClientesController(
                criarCliente,
                listarClientes,
                buscarCliente,
                atualizarCliente,
                criarVeiculo,
                listarVeiculosDoCliente,
                presenter);
    }

    @Produces
    VeiculosController veiculosController(
            BuscarVeiculoUseCase buscarVeiculo,
            AtualizarVeiculoUseCase atualizarVeiculo,
            AtendimentoPresenterAdapter presenter) {
        return new VeiculosController(buscarVeiculo, atualizarVeiculo, presenter);
    }

    @Produces
    OrdensServicoController ordensServicoController(
            AbrirOrdemServicoUseCase abrirOrdemServico,
            ListarOrdensServicoUseCase listarOrdensServico,
            BuscarOrdemServicoUseCase buscarOrdemServico,
            ConsultarHistoricoOrdemServicoUseCase consultarHistoricoOrdemServico,
            AlterarEstadoOrdemServicoUseCase alterarEstadoOrdemServico,
            CancelarOrdemServicoUseCase cancelarOrdemServico,
            IncluirServicoOrdemServicoUseCase incluirServico,
            IncluirPecaOrdemServicoUseCase incluirPeca,
            AtendimentoPresenterAdapter presenter) {
        return new OrdensServicoController(
                abrirOrdemServico,
                listarOrdensServico,
                buscarOrdemServico,
                consultarHistoricoOrdemServico,
                alterarEstadoOrdemServico,
                cancelarOrdemServico,
                incluirServico,
                incluirPeca,
                presenter);
    }

    @Produces
    StatusController statusController(
            @ConfigProperty(name = "quarkus.application.name") String applicationName,
            @ConfigProperty(name = "oficina.observability.deployment-environment") String environment) {
        return new StatusController(applicationName, environment);
    }

    @Produces
    @RequestScoped
    AtendimentoPresenterAdapter atendimentoPresenter() {
        return new AtendimentoPresenterAdapter();
    }
}

package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.usecases.ordem_de_servico.AbrirOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.AlterarEstadoOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.BuscarOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.CancelarOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.ConsultarHistoricoOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.ListarOrdensServicoUseCase;
import br.com.oficina.os.interfaces.presenters.AtendimentoPresenterAdapter;
import br.com.oficina.os.interfaces.presenters.view_model.HistoricoOrdemServicoViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.OperacaoAssincronaViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.OrdemServicoViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.PageResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OrdensServicoController {
    private final AbrirOrdemServicoUseCase abrirOrdemServico;
    private final ListarOrdensServicoUseCase listarOrdensServico;
    private final BuscarOrdemServicoUseCase buscarOrdemServico;
    private final ConsultarHistoricoOrdemServicoUseCase consultarHistoricoOrdemServico;
    private final AlterarEstadoOrdemServicoUseCase alterarEstadoOrdemServico;
    private final CancelarOrdemServicoUseCase cancelarOrdemServico;
    private final AtendimentoPresenterAdapter presenter;

    public OrdensServicoController(
            AbrirOrdemServicoUseCase abrirOrdemServico,
            ListarOrdensServicoUseCase listarOrdensServico,
            BuscarOrdemServicoUseCase buscarOrdemServico,
            ConsultarHistoricoOrdemServicoUseCase consultarHistoricoOrdemServico,
            AlterarEstadoOrdemServicoUseCase alterarEstadoOrdemServico,
            CancelarOrdemServicoUseCase cancelarOrdemServico,
            AtendimentoPresenterAdapter presenter) {
        this.abrirOrdemServico = abrirOrdemServico;
        this.listarOrdensServico = listarOrdensServico;
        this.buscarOrdemServico = buscarOrdemServico;
        this.consultarHistoricoOrdemServico = consultarHistoricoOrdemServico;
        this.alterarEstadoOrdemServico = alterarEstadoOrdemServico;
        this.cancelarOrdemServico = cancelarOrdemServico;
        this.presenter = presenter;
    }

    public CompletableFuture<OrdemServicoViewModel> abrirOrdemServico(OrdemServicoCreateRequest request) {
        var command = new AbrirOrdemServicoUseCase.Command(
                request.clienteId(),
                request.veiculoId(),
                request.descricaoProblema());
        return abrirOrdemServico.executar(command)
                .thenApply(presenter::ordemServico);
    }

    public CompletableFuture<PageResponse<OrdemServicoViewModel>> consultarOrdensServico(
            TipoDeEstadoDaOrdemDeServico estado,
            Integer page,
            Integer size) {
        return listarOrdensServico.executar(estado)
                .thenApply(ordens -> ordens.stream()
                        .map(presenter::ordemServico)
                        .toList())
                .thenApply(ordens -> PageResponse.of(ordens, page == null ? 0 : page, size == null ? 20 : size));
    }

    public CompletableFuture<OrdemServicoViewModel> consultarOrdemServico(UUID ordemServicoId) {
        return buscarOrdemServico.executar(ordemServicoId).thenApply(presenter::ordemServico);
    }

    public CompletableFuture<List<HistoricoOrdemServicoViewModel>> consultarHistoricoOrdemServico(UUID ordemServicoId) {
        return consultarHistoricoOrdemServico.executar(ordemServicoId)
                .thenApply(historicos -> historicos.stream()
                        .map(presenter::historico)
                        .toList());
    }

    public CompletableFuture<OrdemServicoViewModel> alterarEstadoOrdemServico(
            UUID ordemServicoId,
            AlterarEstadoRequest request) {
        var command = new AlterarEstadoOrdemServicoUseCase.Command(
                ordemServicoId,
                request.estado(),
                request.motivo());
        return alterarEstadoOrdemServico.executar(command)
                .thenApply(presenter::ordemServico);
    }

    public CompletableFuture<OperacaoAssincronaViewModel> cancelarOrdemServico(
            UUID ordemServicoId,
            CancelamentoRequest request) {
        var command = new CancelarOrdemServicoUseCase.Command(
                ordemServicoId,
                request == null ? null : request.motivo());
        return cancelarOrdemServico.executar(command)
                .thenApply(operacao -> new OperacaoAssincronaViewModel(operacao.status(), operacao.solicitadoEm()));
    }

    public record OrdemServicoCreateRequest(
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema) {
    }

    public record AlterarEstadoRequest(
            TipoDeEstadoDaOrdemDeServico estado,
            String motivo) {
    }

    public record CancelamentoRequest(String motivo) {
    }
}

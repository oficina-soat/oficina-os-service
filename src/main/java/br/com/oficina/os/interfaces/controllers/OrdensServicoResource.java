package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.usecases.ordem_de_servico.AbrirOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.AlterarEstadoOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.BuscarOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.CancelarOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.ConsultarHistoricoOrdemServicoUseCase;
import br.com.oficina.os.core.usecases.ordem_de_servico.ListarOrdensServicoUseCase;
import br.com.oficina.os.interfaces.presenters.AtendimentoPresenter;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/ordens-servico")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class OrdensServicoResource {
    private final AbrirOrdemServicoUseCase abrirOrdemServico;
    private final ListarOrdensServicoUseCase listarOrdensServico;
    private final BuscarOrdemServicoUseCase buscarOrdemServico;
    private final ConsultarHistoricoOrdemServicoUseCase consultarHistoricoOrdemServico;
    private final AlterarEstadoOrdemServicoUseCase alterarEstadoOrdemServico;
    private final CancelarOrdemServicoUseCase cancelarOrdemServico;
    private final AtendimentoPresenter presenter;

    @Inject
    public OrdensServicoResource(
            AbrirOrdemServicoUseCase abrirOrdemServico,
            ListarOrdensServicoUseCase listarOrdensServico,
            BuscarOrdemServicoUseCase buscarOrdemServico,
            ConsultarHistoricoOrdemServicoUseCase consultarHistoricoOrdemServico,
            AlterarEstadoOrdemServicoUseCase alterarEstadoOrdemServico,
            CancelarOrdemServicoUseCase cancelarOrdemServico,
            AtendimentoPresenter presenter) {
        this.abrirOrdemServico = abrirOrdemServico;
        this.listarOrdensServico = listarOrdensServico;
        this.buscarOrdemServico = buscarOrdemServico;
        this.consultarHistoricoOrdemServico = consultarHistoricoOrdemServico;
        this.alterarEstadoOrdemServico = alterarEstadoOrdemServico;
        this.cancelarOrdemServico = cancelarOrdemServico;
        this.presenter = presenter;
    }

    @POST
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response abrirOrdemServico(OrdemServicoCreateRequest request) {
        var ordem = presenter.ordemServico(abrirOrdemServico.executar(
                request.clienteId(),
                request.veiculoId(),
                request.descricaoProblema()));
        return Response.created(URI.create("/api/v1/ordens-servico/" + ordem.ordemServicoId()))
                .entity(ordem)
                .build();
    }

    @GET
    public PageResponse<OrdemServicoResponse> consultarOrdensServico(
            @QueryParam("estado") TipoDeEstadoDaOrdemDeServico estado,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        var ordens = listarOrdensServico.executar(estado).stream()
                .map(presenter::ordemServico)
                .toList();
        return PageResponse.of(ordens, page == null ? 0 : page, size == null ? 20 : size);
    }

    @GET
    @Path("{ordemServicoId}")
    public OrdemServicoResponse consultarOrdemServico(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return presenter.ordemServico(buscarOrdemServico.executar(ordemServicoId));
    }

    @GET
    @Path("{ordemServicoId}/historico")
    public List<HistoricoOrdemServicoResponse> consultarHistoricoOrdemServico(
            @PathParam("ordemServicoId") UUID ordemServicoId) {
        return consultarHistoricoOrdemServico.executar(ordemServicoId).stream()
                .map(presenter::historico)
                .toList();
    }

    @PATCH
    @Path("{ordemServicoId}/estado")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public OrdemServicoResponse alterarEstadoOrdemServico(
            @PathParam("ordemServicoId") UUID ordemServicoId,
            AlterarEstadoRequest request) {
        return presenter.ordemServico(alterarEstadoOrdemServico.executar(
                ordemServicoId,
                request.estado(),
                request.motivo()));
    }

    @POST
    @Path("{ordemServicoId}/cancelamento")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response cancelarOrdemServico(
            @PathParam("ordemServicoId") UUID ordemServicoId,
            CancelamentoRequest request) {
        var operacao = cancelarOrdemServico.executar(ordemServicoId, request == null ? null : request.motivo());
        return Response.accepted(new OperacaoAssincronaResponse(operacao.status(), operacao.solicitadoEm())).build();
    }

    public record OrdemServicoCreateRequest(
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema) {
    }

    public record OrdemServicoResponse(
            UUID ordemServicoId,
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema,
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record AlterarEstadoRequest(
            TipoDeEstadoDaOrdemDeServico estado,
            String motivo) {
    }

    public record HistoricoOrdemServicoResponse(
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime dataDoEstado,
            String motivo) {
    }

    public record CancelamentoRequest(String motivo) {
    }

    public record OperacaoAssincronaResponse(
            String status,
            OffsetDateTime solicitadoEm) {
    }
}

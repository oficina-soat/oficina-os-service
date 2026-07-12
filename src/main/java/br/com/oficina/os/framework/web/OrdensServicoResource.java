package br.com.oficina.os.framework.web;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.interfaces.controllers.OrdensServicoController;
import br.com.oficina.os.interfaces.presenters.view_model.HistoricoOrdemServicoViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.OperacaoAssincronaViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.OrdemServicoViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.PageResponse;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
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
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/api/v1/ordens-servico")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Blocking
public class OrdensServicoResource {
    private final OrdensServicoController ordensServicoController;

    @Inject
    public OrdensServicoResource(OrdensServicoController ordensServicoController) {
        this.ordensServicoController = ordensServicoController;
    }

    @POST
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> abrirOrdemServico(OrdensServicoController.OrdemServicoCreateRequest request) {
        return Uni.createFrom()
                .completionStage(ordensServicoController.abrirOrdemServico(request))
                .map(ordem -> Response.created(URI.create("/api/v1/ordens-servico/" + ordem.ordemServicoId()))
                        .entity(ordem)
                        .build());
    }

    @GET
    public Uni<PageResponse<OrdemServicoViewModel>> consultarOrdensServico(
            @QueryParam("estado") TipoDeEstadoDaOrdemDeServico estado,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return Uni.createFrom().completionStage(ordensServicoController.consultarOrdensServico(estado, page, size));
    }

    @GET
    @Path("{ordemServicoId}")
    public Uni<OrdemServicoViewModel> consultarOrdemServico(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return Uni.createFrom().completionStage(ordensServicoController.consultarOrdemServico(ordemServicoId));
    }

    @GET
    @Path("{ordemServicoId}/historico")
    public Uni<List<HistoricoOrdemServicoViewModel>> consultarHistoricoOrdemServico(
            @PathParam("ordemServicoId") UUID ordemServicoId) {
        return Uni.createFrom().completionStage(ordensServicoController.consultarHistoricoOrdemServico(ordemServicoId));
    }

    @PATCH
    @Path("{ordemServicoId}/estado")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<OrdemServicoViewModel> alterarEstadoOrdemServico(
            @PathParam("ordemServicoId") UUID ordemServicoId,
            OrdensServicoController.AlterarEstadoRequest request) {
        return Uni.createFrom().completionStage(ordensServicoController.alterarEstadoOrdemServico(ordemServicoId, request));
    }

    @POST
    @Path("{ordemServicoId}/cancelamento")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> cancelarOrdemServico(
            @PathParam("ordemServicoId") UUID ordemServicoId,
            OrdensServicoController.CancelamentoRequest request) {
        return Uni.createFrom()
                .completionStage(ordensServicoController.cancelarOrdemServico(ordemServicoId, request))
                .map(operacao -> Response.accepted(new OperacaoAssincronaViewModel(
                                operacao.status(),
                                operacao.solicitadoEm()))
                        .build());
    }
}

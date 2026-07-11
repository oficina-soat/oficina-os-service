package br.com.oficina.os.framework.web;

import br.com.oficina.os.interfaces.controllers.ClientesController;
import br.com.oficina.os.interfaces.controllers.VeiculosController;
import br.com.oficina.os.interfaces.presenters.view_model.ClienteViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.PageResponse;
import br.com.oficina.os.interfaces.presenters.view_model.VeiculoViewModel;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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

@Path("/api/v1/clientes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class ClientesResource {
    private final ClientesController clientesController;

    @Inject
    public ClientesResource(ClientesController clientesController) {
        this.clientesController = clientesController;
    }

    @POST
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> criarCliente(ClientesController.ClienteCreateRequest request) {
        return Uni.createFrom()
                .completionStage(clientesController.criarCliente(request))
                .map(cliente -> Response.created(URI.create("/api/v1/clientes/" + cliente.clienteId()))
                        .entity(cliente)
                        .build());
    }

    @GET
    public Uni<PageResponse<ClienteViewModel>> consultarClientes(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return Uni.createFrom().completionStage(clientesController.consultarClientes(page, size));
    }

    @GET
    @Path("{clienteId}")
    public Uni<ClienteViewModel> consultarCliente(@PathParam("clienteId") UUID clienteId) {
        return Uni.createFrom().completionStage(clientesController.consultarCliente(clienteId));
    }

    @PUT
    @Path("{clienteId}")
    public Uni<ClienteViewModel> atualizarCliente(
            @PathParam("clienteId") UUID clienteId,
            ClientesController.ClienteUpdateRequest request) {
        return Uni.createFrom().completionStage(clientesController.atualizarCliente(clienteId, request));
    }

    @POST
    @Path("{clienteId}/veiculos")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> criarVeiculo(
            @PathParam("clienteId") UUID clienteId,
            VeiculosController.VeiculoCreateRequest request) {
        return Uni.createFrom()
                .completionStage(clientesController.criarVeiculo(clienteId, request))
                .map(veiculo -> Response.created(URI.create("/api/v1/veiculos/" + veiculo.veiculoId()))
                        .entity(veiculo)
                        .build());
    }

    @GET
    @Path("{clienteId}/veiculos")
    public Uni<List<VeiculoViewModel>> consultarVeiculosDoCliente(@PathParam("clienteId") UUID clienteId) {
        return Uni.createFrom().completionStage(clientesController.consultarVeiculosDoCliente(clienteId));
    }
}

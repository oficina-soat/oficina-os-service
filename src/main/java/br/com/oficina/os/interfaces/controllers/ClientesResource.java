package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import br.com.oficina.os.interfaces.presenters.AtendimentoPresenter;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/clientes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class ClientesResource {
    @Inject
    AtendimentoSeedStore store;

    @Inject
    AtendimentoPresenter presenter;

    @POST
    public Response criarCliente(ClienteCreateRequest request) {
        var cliente = presenter.cliente(store.criarCliente(
                request.nome(),
                request.documento(),
                request.telefone(),
                request.email()));
        return Response.created(URI.create("/api/v1/clientes/" + cliente.clienteId()))
                .entity(cliente)
                .build();
    }

    @GET
    public PageResponse<ClienteResponse> consultarClientes(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        var clientes = store.listarClientes().stream()
                .map(presenter::cliente)
                .toList();
        return PageResponse.of(clientes, page == null ? 0 : page, size == null ? 20 : size);
    }

    @GET
    @Path("{clienteId}")
    public ClienteResponse consultarCliente(@PathParam("clienteId") UUID clienteId) {
        return presenter.cliente(store.buscarCliente(clienteId));
    }

    @PUT
    @Path("{clienteId}")
    public ClienteResponse atualizarCliente(
            @PathParam("clienteId") UUID clienteId,
            ClienteUpdateRequest request) {
        return presenter.cliente(store.atualizarCliente(
                clienteId,
                request.nome(),
                request.documento(),
                request.telefone(),
                request.email()));
    }

    @POST
    @Path("{clienteId}/veiculos")
    public Response criarVeiculo(
            @PathParam("clienteId") UUID clienteId,
            VeiculosResource.VeiculoCreateRequest request) {
        var veiculo = presenter.veiculo(store.criarVeiculo(
                clienteId,
                request.placa(),
                request.marca(),
                request.modelo(),
                request.ano()));
        return Response.created(URI.create("/api/v1/veiculos/" + veiculo.veiculoId()))
                .entity(veiculo)
                .build();
    }

    @GET
    @Path("{clienteId}/veiculos")
    public List<VeiculosResource.VeiculoResponse> consultarVeiculosDoCliente(@PathParam("clienteId") UUID clienteId) {
        return store.listarVeiculosDoCliente(clienteId).stream()
                .map(presenter::veiculo)
                .toList();
    }

    public record ClienteCreateRequest(
            String nome,
            String documento,
            String telefone,
            String email) {
    }

    public record ClienteUpdateRequest(
            String nome,
            String documento,
            String telefone,
            String email) {
    }

    public record ClienteResponse(
            UUID clienteId,
            String nome,
            String documento,
            String telefone,
            String email,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }
}

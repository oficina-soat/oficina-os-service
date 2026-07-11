package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.core.usecases.cliente.AtualizarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.BuscarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.CriarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.ListarClientesUseCase;
import br.com.oficina.os.core.usecases.veiculo.CriarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.ListarVeiculosDoClienteUseCase;
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
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/clientes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class ClientesResource {
    private final CriarClienteUseCase criarCliente;
    private final ListarClientesUseCase listarClientes;
    private final BuscarClienteUseCase buscarCliente;
    private final AtualizarClienteUseCase atualizarCliente;
    private final CriarVeiculoUseCase criarVeiculo;
    private final ListarVeiculosDoClienteUseCase listarVeiculosDoCliente;
    private final AtendimentoPresenter presenter;

    @Inject
    public ClientesResource(
            CriarClienteUseCase criarCliente,
            ListarClientesUseCase listarClientes,
            BuscarClienteUseCase buscarCliente,
            AtualizarClienteUseCase atualizarCliente,
            CriarVeiculoUseCase criarVeiculo,
            ListarVeiculosDoClienteUseCase listarVeiculosDoCliente,
            AtendimentoPresenter presenter) {
        this.criarCliente = criarCliente;
        this.listarClientes = listarClientes;
        this.buscarCliente = buscarCliente;
        this.atualizarCliente = atualizarCliente;
        this.criarVeiculo = criarVeiculo;
        this.listarVeiculosDoCliente = listarVeiculosDoCliente;
        this.presenter = presenter;
    }

    @POST
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response criarCliente(ClienteCreateRequest request) {
        var cliente = presenter.cliente(criarCliente.executar(
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
        var clientes = listarClientes.executar().stream()
                .map(presenter::cliente)
                .toList();
        return PageResponse.of(clientes, page == null ? 0 : page, size == null ? 20 : size);
    }

    @GET
    @Path("{clienteId}")
    public ClienteResponse consultarCliente(@PathParam("clienteId") UUID clienteId) {
        return presenter.cliente(buscarCliente.executar(clienteId));
    }

    @PUT
    @Path("{clienteId}")
    public ClienteResponse atualizarCliente(
            @PathParam("clienteId") UUID clienteId,
            ClienteUpdateRequest request) {
        return presenter.cliente(atualizarCliente.executar(
                clienteId,
                request.nome(),
                request.documento(),
                request.telefone(),
                request.email()));
    }

    @POST
    @Path("{clienteId}/veiculos")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response criarVeiculo(
            @PathParam("clienteId") UUID clienteId,
            VeiculosResource.VeiculoCreateRequest request) {
        var veiculo = presenter.veiculo(criarVeiculo.executar(
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
        return listarVeiculosDoCliente.executar(clienteId).stream()
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

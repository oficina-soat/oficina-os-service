package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.core.usecases.cliente.AtualizarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.BuscarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.CriarClienteUseCase;
import br.com.oficina.os.core.usecases.cliente.ListarClientesUseCase;
import br.com.oficina.os.core.usecases.veiculo.CriarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.ListarVeiculosDoClienteUseCase;
import br.com.oficina.os.interfaces.presenters.AtendimentoPresenterAdapter;
import br.com.oficina.os.interfaces.presenters.view_model.ClienteViewModel;
import br.com.oficina.os.interfaces.presenters.view_model.PageResponse;
import br.com.oficina.os.interfaces.presenters.view_model.VeiculoViewModel;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClientesController {
    private final CriarClienteUseCase criarCliente;
    private final ListarClientesUseCase listarClientes;
    private final BuscarClienteUseCase buscarCliente;
    private final AtualizarClienteUseCase atualizarCliente;
    private final CriarVeiculoUseCase criarVeiculo;
    private final ListarVeiculosDoClienteUseCase listarVeiculosDoCliente;
    private final AtendimentoPresenterAdapter presenter;

    public ClientesController(
            CriarClienteUseCase criarCliente,
            ListarClientesUseCase listarClientes,
            BuscarClienteUseCase buscarCliente,
            AtualizarClienteUseCase atualizarCliente,
            CriarVeiculoUseCase criarVeiculo,
            ListarVeiculosDoClienteUseCase listarVeiculosDoCliente,
            AtendimentoPresenterAdapter presenter) {
        this.criarCliente = criarCliente;
        this.listarClientes = listarClientes;
        this.buscarCliente = buscarCliente;
        this.atualizarCliente = atualizarCliente;
        this.criarVeiculo = criarVeiculo;
        this.listarVeiculosDoCliente = listarVeiculosDoCliente;
        this.presenter = presenter;
    }

    public CompletableFuture<ClienteViewModel> criarCliente(ClienteCreateRequest request) {
        var command = new CriarClienteUseCase.Command(
                request.nome(),
                request.documento(),
                request.telefone(),
                request.email());
        return criarCliente.executar(command)
                .thenApply(presenter::cliente);
    }

    public CompletableFuture<PageResponse<ClienteViewModel>> consultarClientes(
            Integer page, Integer size, String nome, String documento, String email) {
        return listarClientes.executar(new ListarClientesUseCase.Query(nome, documento, email))
                .thenApply(clientes -> clientes.stream()
                        .map(presenter::cliente)
                        .toList())
                .thenApply(clientes -> PageResponse.of(clientes, page == null ? 0 : page, size == null ? 20 : size));
    }

    public CompletableFuture<ClienteViewModel> consultarCliente(UUID clienteId) {
        return buscarCliente.executar(clienteId).thenApply(presenter::cliente);
    }

    public CompletableFuture<ClienteViewModel> atualizarCliente(UUID clienteId, ClienteUpdateRequest request) {
        var command = new AtualizarClienteUseCase.Command(
                clienteId,
                request.nome(),
                request.documento(),
                request.telefone(),
                request.email());
        return atualizarCliente.executar(command)
                .thenApply(presenter::cliente);
    }

    public CompletableFuture<VeiculoViewModel> criarVeiculo(UUID clienteId, VeiculosController.VeiculoCreateRequest request) {
        var command = new CriarVeiculoUseCase.Command(
                clienteId,
                request.placa(),
                request.marca(),
                request.modelo(),
                request.ano());
        return criarVeiculo.executar(command)
                .thenApply(presenter::veiculo);
    }

    public CompletableFuture<List<VeiculoViewModel>> consultarVeiculosDoCliente(UUID clienteId) {
        return listarVeiculosDoCliente.executar(clienteId)
                .thenApply(veiculos -> veiculos.stream()
                        .map(presenter::veiculo)
                        .toList());
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
}

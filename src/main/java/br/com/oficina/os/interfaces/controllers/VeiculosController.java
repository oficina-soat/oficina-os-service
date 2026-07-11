package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.core.usecases.veiculo.AtualizarVeiculoUseCase;
import br.com.oficina.os.core.usecases.veiculo.BuscarVeiculoUseCase;
import br.com.oficina.os.interfaces.presenters.AtendimentoPresenterAdapter;
import br.com.oficina.os.interfaces.presenters.view_model.VeiculoViewModel;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VeiculosController {
    private final BuscarVeiculoUseCase buscarVeiculo;
    private final AtualizarVeiculoUseCase atualizarVeiculo;
    private final AtendimentoPresenterAdapter presenter;

    public VeiculosController(
            BuscarVeiculoUseCase buscarVeiculo,
            AtualizarVeiculoUseCase atualizarVeiculo,
            AtendimentoPresenterAdapter presenter) {
        this.buscarVeiculo = buscarVeiculo;
        this.atualizarVeiculo = atualizarVeiculo;
        this.presenter = presenter;
    }

    public CompletableFuture<VeiculoViewModel> consultarVeiculo(UUID veiculoId) {
        return buscarVeiculo.executar(veiculoId).thenApply(presenter::veiculo);
    }

    public CompletableFuture<VeiculoViewModel> atualizarVeiculo(UUID veiculoId, VeiculoUpdateRequest request) {
        var command = new AtualizarVeiculoUseCase.Command(
                veiculoId,
                request.placa(),
                request.marca(),
                request.modelo(),
                request.ano());
        return atualizarVeiculo.executar(command)
                .thenApply(presenter::veiculo);
    }

    public record VeiculoCreateRequest(
            String placa,
            String marca,
            String modelo,
            int ano) {
    }

    public record VeiculoUpdateRequest(
            String placa,
            String marca,
            String modelo,
            int ano) {
    }
}

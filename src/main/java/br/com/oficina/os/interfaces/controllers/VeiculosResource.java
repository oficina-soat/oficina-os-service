package br.com.oficina.os.interfaces.controllers;

import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import br.com.oficina.os.interfaces.presenters.AtendimentoPresenter;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.UUID;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Path("/api/v1")
public class VeiculosResource {
    @Inject
    AtendimentoSeedStore store;

    @Inject
    AtendimentoPresenter presenter;

    @GET
    @Path("veiculos/{veiculoId}")
    public VeiculoResponse consultarVeiculo(@PathParam("veiculoId") UUID veiculoId) {
        return presenter.veiculo(store.buscarVeiculo(veiculoId));
    }

    @PUT
    @Path("veiculos/{veiculoId}")
    public VeiculoResponse atualizarVeiculo(
            @PathParam("veiculoId") UUID veiculoId,
            VeiculoUpdateRequest request) {
        return presenter.veiculo(store.atualizarVeiculo(
                veiculoId,
                request.placa(),
                request.marca(),
                request.modelo(),
                request.ano()));
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

    public record VeiculoResponse(
            UUID veiculoId,
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            int ano,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }
}

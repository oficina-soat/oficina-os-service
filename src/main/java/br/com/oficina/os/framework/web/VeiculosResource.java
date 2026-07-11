package br.com.oficina.os.framework.web;

import br.com.oficina.os.interfaces.controllers.VeiculosController;
import br.com.oficina.os.interfaces.presenters.view_model.VeiculoViewModel;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Path("/api/v1")
public class VeiculosResource {
    private final VeiculosController veiculosController;

    @Inject
    public VeiculosResource(VeiculosController veiculosController) {
        this.veiculosController = veiculosController;
    }

    @GET
    @Path("veiculos/{veiculoId}")
    public Uni<VeiculoViewModel> consultarVeiculo(@PathParam("veiculoId") UUID veiculoId) {
        return Uni.createFrom().completionStage(veiculosController.consultarVeiculo(veiculoId));
    }

    @PUT
    @Path("veiculos/{veiculoId}")
    public Uni<VeiculoViewModel> atualizarVeiculo(
            @PathParam("veiculoId") UUID veiculoId,
            VeiculosController.VeiculoUpdateRequest request) {
        return Uni.createFrom().completionStage(veiculosController.atualizarVeiculo(veiculoId, request));
    }
}

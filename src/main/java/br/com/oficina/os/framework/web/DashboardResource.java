package br.com.oficina.os.framework.web;

import br.com.oficina.os.core.entities.usuario.TipoDePapelValues;
import br.com.oficina.os.core.usecases.dashboard.ConsultarDashboardOperacionalUseCase;
import br.com.oficina.os.core.usecases.dashboard.ConsultarDashboardOperacionalUseCase.DashboardOrdens;
import br.com.oficina.os.core.usecases.dashboard.ConsultarDashboardOperacionalUseCase.DashboardUsuarios;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class DashboardResource {
    private final ConsultarDashboardOperacionalUseCase useCase;

    @Inject
    public DashboardResource(ConsultarDashboardOperacionalUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    @Path("/ordens-servico")
    @RolesAllowed({TipoDePapelValues.ADMINISTRATIVO, TipoDePapelValues.MECANICO, TipoDePapelValues.RECEPCIONISTA})
    public Uni<DashboardOrdens> consultarOrdens() {
        return Uni.createFrom().completionStage(useCase.executar());
    }

    @GET
    @Path("/usuarios")
    @RolesAllowed(TipoDePapelValues.ADMINISTRATIVO)
    public Uni<DashboardUsuarios> consultarUsuarios() {
        return Uni.createFrom().completionStage(useCase.executarUsuarios());
    }
}

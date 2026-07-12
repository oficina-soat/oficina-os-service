package br.com.oficina.os.framework.web;

import br.com.oficina.os.interfaces.controllers.StatusController;
import br.com.oficina.os.interfaces.presenters.view_model.StatusViewModel;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/api/v1/status")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class StatusResource {
    private final StatusController statusController;

    @Inject
    public StatusResource(StatusController statusController) {
        this.statusController = statusController;
    }

    @GET
    @PermitAll
    public Uni<StatusViewModel> status() {
        return Uni.createFrom().completionStage(statusController.status());
    }

    @POST
    @PermitAll
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<StatusViewModel> mutatingStatusProbe() {
        return status();
    }
}

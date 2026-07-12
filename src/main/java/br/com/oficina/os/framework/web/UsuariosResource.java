package br.com.oficina.os.framework.web;

import br.com.oficina.os.core.entities.usuario.TipoDePapelValues;
import br.com.oficina.os.interfaces.controllers.UsuariosController;
import br.com.oficina.os.interfaces.presenters.view_model.PageResponse;
import br.com.oficina.os.interfaces.presenters.view_model.UsuarioViewModel;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/api/v1/usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(TipoDePapelValues.ADMINISTRATIVO)
@Blocking
public class UsuariosResource {
    private final UsuariosController controller;

    @Inject
    public UsuariosResource(UsuariosController controller) {
        this.controller = controller;
    }

    @POST
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> criar(UsuariosController.UsuarioCreateRequest request) {
        return Uni.createFrom()
                .completionStage(controller.criar(request))
                .map(usuario -> Response.created(URI.create("/api/v1/usuarios/" + usuario.usuarioId()))
                        .entity(usuario)
                        .build());
    }

    @GET
    public Uni<PageResponse<UsuarioViewModel>> listar(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return Uni.createFrom().completionStage(controller.listar(page, size));
    }

    @GET
    @Path("{usuarioId}")
    public Uni<UsuarioViewModel> buscar(@PathParam("usuarioId") UUID usuarioId) {
        return Uni.createFrom().completionStage(controller.buscar(usuarioId));
    }

    @PUT
    @Path("{usuarioId}")
    public Uni<UsuarioViewModel> atualizar(
            @PathParam("usuarioId") UUID usuarioId,
            UsuariosController.UsuarioUpdateRequest request) {
        return Uni.createFrom().completionStage(controller.atualizar(usuarioId, request));
    }

    @DELETE
    @Path("{usuarioId}")
    public Uni<Response> inativar(@PathParam("usuarioId") UUID usuarioId) {
        return Uni.createFrom()
                .completionStage(controller.inativar(usuarioId))
                .replaceWith(Response.noContent().build());
    }
}

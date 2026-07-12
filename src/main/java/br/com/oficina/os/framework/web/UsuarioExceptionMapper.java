package br.com.oficina.os.framework.web;

import br.com.oficina.os.core.exceptions.UsuarioException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.MDC;

@Provider
public class UsuarioExceptionMapper implements ExceptionMapper<UsuarioException> {
    @Context
    UriInfo uriInfo;

    @ConfigProperty(name = "quarkus.application.name")
    String serviceName;

    @Override
    public Response toResponse(UsuarioException exception) {
        var responseStatus = exception.kind() == UsuarioException.Kind.NAO_ENCONTRADO
                ? Response.Status.NOT_FOUND
                : Response.Status.CONFLICT;
        var code = exception.kind() == UsuarioException.Kind.NAO_ENCONTRADO
                ? "RESOURCE_NOT_FOUND"
                : "DUPLICATE_RESOURCE";
        var correlationId = correlationId();
        var timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        var body = new ErrorResponse(
                timestamp,
                responseStatus.getStatusCode(),
                responseStatus.getReasonPhrase(),
                code,
                exception.getMessage(),
                path(),
                correlationId,
                null,
                null,
                null,
                serviceName,
                serviceName + "/" + timestamp.toLocalDate() + "/" + correlationId,
                List.of());
        return Response.status(responseStatus)
                .type(MediaType.APPLICATION_JSON)
                .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                .entity(body)
                .build();
    }

    private String correlationId() {
        var value = MDC.get(CorrelationIdFilter.PROPERTY_NAME);
        return value == null ? UUID.randomUUID().toString() : value.toString();
    }

    private String path() {
        return uriInfo == null ? null : "/" + uriInfo.getPath().replaceFirst("^/+", "");
    }
}

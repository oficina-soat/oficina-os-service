package br.com.oficina.os.framework.web;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.time.OffsetDateTime;
import java.util.List;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        Object correlationId = MDC.get(CorrelationIdFilter.PROPERTY_NAME);
        var body = new ErrorResponse(
                "https://oficina.example/errors/validation",
                "Bad Request",
                Response.Status.BAD_REQUEST.getStatusCode(),
                exception.getMessage(),
                uriInfo == null ? null : uriInfo.getPath(),
                "VALIDATION_ERROR",
                correlationId == null ? null : correlationId.toString(),
                OffsetDateTime.now(),
                List.of());

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}

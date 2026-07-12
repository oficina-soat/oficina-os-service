package br.com.oficina.os.framework.web;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class SecurityErrorResponseFilter implements ContainerResponseFilter {
    @ConfigProperty(name = "quarkus.application.name")
    String serviceName;

    @Override
    public void filter(
            ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) throws IOException {
        var status = responseContext.getStatus();
        if ((status != Response.Status.UNAUTHORIZED.getStatusCode()
                && status != Response.Status.FORBIDDEN.getStatusCode())
                || responseContext.hasEntity()) {
            return;
        }

        var correlationId = correlationId(requestContext);
        var timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        var unauthorized = status == Response.Status.UNAUTHORIZED.getStatusCode();
        var credentialPresent = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null;
        var code = securityCode(unauthorized, credentialPresent);
        var message = securityMessage(unauthorized, credentialPresent);
        var responseStatus = unauthorized ? Response.Status.UNAUTHORIZED : Response.Status.FORBIDDEN;
        var body = new ErrorResponse(
                timestamp,
                status,
                responseStatus.getReasonPhrase(),
                code,
                message,
                path(requestContext),
                correlationId,
                null,
                null,
                null,
                serviceName,
                serviceName + "/" + timestamp.toLocalDate() + "/" + correlationId,
                List.of());

        responseContext.setEntity(body, new Annotation[0], MediaType.APPLICATION_JSON_TYPE);
        responseContext.getHeaders().putSingle(CorrelationIdFilter.HEADER_NAME, correlationId);
    }

    private static String correlationId(ContainerRequestContext requestContext) {
        var value = requestContext.getProperty(CorrelationIdFilter.PROPERTY_NAME);
        if (value != null) {
            return value.toString();
        }
        var headerValue = requestContext.getHeaderString(CorrelationIdFilter.HEADER_NAME);
        return headerValue == null || headerValue.isBlank() ? UUID.randomUUID().toString() : headerValue.trim();
    }

    private static String path(ContainerRequestContext requestContext) {
        var path = requestContext.getUriInfo().getPath();
        return path == null || path.isBlank() ? "/" : "/" + path.replaceFirst("^/+", "");
    }

    private static String securityCode(boolean unauthorized, boolean credentialPresent) {
        if (!unauthorized) {
            return "ACCESS_DENIED";
        }
        return credentialPresent ? "AUTHENTICATION_INVALID" : "AUTHENTICATION_REQUIRED";
    }

    private static String securityMessage(boolean unauthorized, boolean credentialPresent) {
        if (!unauthorized) {
            return "Usuário autenticado sem permissão para a operação.";
        }
        return credentialPresent
                ? "Token JWT inválido, expirado ou incompatível."
                : "Token JWT obrigatório.";
    }
}

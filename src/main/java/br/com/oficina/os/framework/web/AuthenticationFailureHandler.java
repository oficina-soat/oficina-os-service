package br.com.oficina.os.framework.web;

import br.com.oficina.os.framework.observability.StructuredLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.AuthenticationException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthenticationFailureHandler {
    private static final Logger LOG = Logger.getLogger(AuthenticationFailureHandler.class);

    private final ObjectMapper objectMapper;
    private final String serviceName;

    @Inject
    public AuthenticationFailureHandler(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "quarkus.application.name") String serviceName) {
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
    }

    void register(@Observes Router router) {
        router.route().last().failureHandler(this::handle);
    }

    private void handle(RoutingContext context) {
        if (context.response().ended()) {
            return;
        }
        if (context.response().getStatusCode() != 401
                && !(rootCause(context.failure()) instanceof AuthenticationException)) {
            context.next();
            return;
        }

        var correlationId = correlationId(context);
        var timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        var body = new ErrorResponse(
                timestamp,
                401,
                "Unauthorized",
                "AUTHENTICATION_INVALID",
                "Token JWT inválido, expirado ou incompatível.",
                context.request().path(),
                correlationId,
                null,
                null,
                null,
                serviceName,
                serviceName + "/" + timestamp.toLocalDate() + "/" + correlationId,
                List.of());
        try {
            var json = objectMapper.writeValueAsString(body);
            context.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .putHeader(CorrelationIdFilter.HEADER_NAME, correlationId)
                    .end(json);
            StructuredLog.withFields(Map.of(
                    "correlationId", correlationId,
                    "http.method", context.request().method().name(),
                    "http.path", context.request().path(),
                    "http.status", 401,
                    "error.code", "AUTHENTICATION_INVALID"), () -> LOG.warn("jwt authentication failed"));
        } catch (JsonProcessingException _) {
            context.next();
        }
    }

    private static String correlationId(RoutingContext context) {
        var value = context.request().getHeader(CorrelationIdFilter.HEADER_NAME);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
    }

    private static Throwable rootCause(Throwable failure) {
        if (failure == null) {
            return null;
        }
        var root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }
}

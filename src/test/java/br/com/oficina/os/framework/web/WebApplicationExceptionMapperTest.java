package br.com.oficina.os.framework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Proxy;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebApplicationExceptionMapperTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void deveMapearNotFoundComCorrelationIdExistente() {
        MDC.put(CorrelationIdFilter.PROPERTY_NAME, "corr-web-001");
        var mapper = mapper(uriInfo("/api/v1/clientes"));

        var response = mapper.toResponse(new NotFoundException("Cliente nao encontrado"));
        var body = assertInstanceOf(ErrorResponse.class, response.getEntity());

        assertEquals(404, response.getStatus());
        assertEquals("corr-web-001", response.getHeaderString(CorrelationIdFilter.HEADER_NAME));
        assertEquals("RESOURCE_NOT_FOUND", body.code());
        assertEquals("/api/v1/clientes", body.path());
        assertEquals("oficina-os-service", body.service());
        assertEquals("corr-web-001", body.correlationId());
    }

    @Test
    void deveMapearConflitoDeTransicaoInvalida() {
        var mapper = mapper(uriInfo("api/v1/ordens-servico"));
        var exception = new WebApplicationException(
                "Transicao de estado invalida: RECEBIDA -> ENTREGUE",
                Response.status(Response.Status.CONFLICT).build());

        var response = mapper.toResponse(exception);
        var body = assertInstanceOf(ErrorResponse.class, response.getEntity());

        assertEquals(409, response.getStatus());
        assertEquals("INVALID_STATE_TRANSITION", body.code());
        assertEquals("/api/v1/ordens-servico", body.path());
    }

    @Test
    void deveMapearFalhaDeIdempotenciaObrigatoria() {
        var mapper = mapper(uriInfo("api/v1/clientes"));

        var response = mapper.toResponse(new BadRequestException("Header Idempotency-Key e obrigatorio."));
        var body = assertInstanceOf(ErrorResponse.class, response.getEntity());

        assertEquals(400, response.getStatus());
        assertEquals("IDEMPOTENCY_KEY_REQUIRED", body.code());
    }

    @Test
    void deveMapearStatusHttpGenericoSemUriInfo() {
        var mapper = mapper(null);
        var exception = new WebApplicationException(
                "Limite excedido",
                Response.status(429).build());

        var response = mapper.toResponse(exception);
        var body = assertInstanceOf(ErrorResponse.class, response.getEntity());

        assertEquals(429, response.getStatus());
        assertEquals("HTTP_429", body.code());
        assertNull(body.path());
    }

    private static WebApplicationExceptionMapper mapper(UriInfo uriInfo) {
        var mapper = new WebApplicationExceptionMapper();
        mapper.serviceName = "oficina-os-service";
        mapper.uriInfo = uriInfo;
        return mapper;
    }

    private static UriInfo uriInfo(String path) {
        return (UriInfo) Proxy.newProxyInstance(
                UriInfo.class.getClassLoader(),
                new Class<?>[] {UriInfo.class},
                (_, method, _) -> {
                    if ("getPath".equals(method.getName()) && method.getParameterCount() == 0) {
                        return path;
                    }
                    if ("toString".equals(method.getName())) {
                        return "UriInfo[" + path + "]";
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class || type == short.class || type == int.class || type == long.class) {
            return 0;
        }
        if (type == float.class || type == double.class) {
            return 0.0;
        }
        return null;
    }
}

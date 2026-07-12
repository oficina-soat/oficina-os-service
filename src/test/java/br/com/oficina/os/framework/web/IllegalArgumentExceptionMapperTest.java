package br.com.oficina.os.framework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Proxy;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IllegalArgumentExceptionMapperTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void deveMapearErroDeValidacaoComCorrelationIdExistente() {
        MDC.put(CorrelationIdFilter.PROPERTY_NAME, "corr-validation-001");
        var mapper = mapper(uriInfo("api/v1/clientes"));

        var response = mapper.toResponse(new IllegalArgumentException("Nome do cliente e obrigatorio."));
        var body = assertInstanceOf(ErrorResponse.class, response.getEntity());

        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", body.code());
        assertEquals("/api/v1/clientes", body.path());
        assertEquals("corr-validation-001", body.correlationId());
        assertEquals("corr-validation-001", response.getHeaderString(CorrelationIdFilter.HEADER_NAME));
    }

    @Test
    void deveGerarCorrelationIdQuandoAusente() {
        var mapper = mapper(null);

        var response = mapper.toResponse(new IllegalArgumentException("Entrada invalida."));
        var body = assertInstanceOf(ErrorResponse.class, response.getEntity());

        assertEquals(400, response.getStatus());
        assertNull(body.path());
        assertNotNull(body.correlationId());
        assertEquals(body.correlationId(), response.getHeaderString(CorrelationIdFilter.HEADER_NAME));
    }

    private static IllegalArgumentExceptionMapper mapper(UriInfo uriInfo) {
        var mapper = new IllegalArgumentExceptionMapper();
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
                    return null;
                });
    }
}

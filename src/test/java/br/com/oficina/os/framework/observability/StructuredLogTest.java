package br.com.oficina.os.framework.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StructuredLogTest {
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldExposeQueryableEventTypeAliases() {
        StructuredLog.withFields(Map.of("eventType", "ordemDeServicoCriada"), () -> {
            assertEquals("ordemDeServicoCriada", MDC.get("eventType"));
            assertEquals("ordemDeServicoCriada", MDC.get("domainEventType"));
            assertEquals("ordemDeServicoCriada", MDC.get("event.type"));
        });
    }
}

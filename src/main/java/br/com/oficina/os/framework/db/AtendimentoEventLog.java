package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.SagaRecord;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import br.com.oficina.os.framework.observability.StructuredLog;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

final class AtendimentoEventLog {
    private static final String CONSUMER = "oficina-os-service";

    private AtendimentoEventLog() {
    }

    static void logEvent(Logger log, String message, OutboxEventRecord event, String messageStatus) {
        StructuredLog.info(log, message, Map.of(
                "correlationId", event.correlationId(),
                "eventId", event.eventId().toString(),
                "eventType", event.eventType(),
                "eventVersion", event.eventVersion(),
                "topic", event.topic(),
                "producer", event.producer(),
                "aggregateId", event.aggregateId().toString(),
                "messageStatus", messageStatus));
    }

    static void logEvent(
            Logger log,
            String message,
            DomainEventEnvelope event,
            String messageStatus,
            UUID aggregateId,
            String correlationId) {
        StructuredLog.info(log, message, Map.of(
                "correlationId", correlationId(correlationId),
                "eventId", event.eventId().toString(),
                "eventType", event.eventType(),
                "eventVersion", event.eventVersion(),
                "producer", event.producer(),
                "consumer", CONSUMER,
                "aggregateId", aggregateId.toString(),
                "messageStatus", messageStatus));
    }

    static String correlationId(SagaRecord saga, DomainEventEnvelope event) {
        if (saga != null && saga.correlationId() != null && !saga.correlationId().isBlank()) {
            return saga.correlationId();
        }
        return event.eventId().toString();
    }

    static String correlationId(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId.trim();
        }
        var mdcCorrelationId = MDC.get("correlationId");
        if (mdcCorrelationId != null && !mdcCorrelationId.toString().isBlank()) {
            return mdcCorrelationId.toString();
        }
        return "local-" + UUID.randomUUID();
    }
}

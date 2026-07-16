package br.com.oficina.os.core.interfaces.messaging;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        OffsetDateTime occurredAt,
        String producer,
        UUID aggregateId,
        String correlationId,
        Map<String, Object> payload) {

    public DomainEventEnvelope(
            UUID eventId,
            String eventType,
            int eventVersion,
            OffsetDateTime occurredAt,
            String producer,
            UUID aggregateId,
            Map<String, Object> payload) {
        this(eventId, eventType, eventVersion, occurredAt, producer, aggregateId, null, payload);
    }
}

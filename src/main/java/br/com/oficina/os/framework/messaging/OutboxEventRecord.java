package br.com.oficina.os.framework.messaging;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record OutboxEventRecord(
        UUID eventId,
        UUID aggregateId,
        String eventType,
        int eventVersion,
        String topic,
        String producer,
        Map<String, Object> payload,
        String status,
        String correlationId,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt,
        int attempts,
        String lastError) {
}

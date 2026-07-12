package br.com.oficina.os.framework.messaging;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import br.com.oficina.os.core.usecases.outbox.PublicarEventosPendentesUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OutboxPublisher {
    private static final Logger LOG = Logger.getLogger(OutboxPublisher.class);

    private final AtendimentoGateway gateway;
    private final PublicarEventosPendentesUseCase publicarEventosPendentes;
    private final AwsDomainMessagingClient messagingClient;
    private final DomainEventJsonCodec codec;
    private final boolean publisherEnabled;
    private final int batchSize;
    private final int maxAttempts;
    private final long backoffBaseMs;

    @Inject
    public OutboxPublisher(
            AtendimentoGateway gateway,
            PublicarEventosPendentesUseCase publicarEventosPendentes,
            AwsDomainMessagingClient messagingClient,
            DomainEventJsonCodec codec,
            @ConfigProperty(name = "oficina.messaging.publisher.enabled", defaultValue = "false") boolean publisherEnabled,
            @ConfigProperty(name = "oficina.messaging.publisher.batch-size", defaultValue = "10") int batchSize,
            @ConfigProperty(name = "oficina.messaging.publisher.max-attempts", defaultValue = "5") int maxAttempts,
            @ConfigProperty(name = "oficina.messaging.publisher.backoff-base-ms", defaultValue = "1000") long backoffBaseMs) {
        this.gateway = gateway;
        this.publicarEventosPendentes = publicarEventosPendentes;
        this.messagingClient = messagingClient;
        this.codec = codec;
        this.publisherEnabled = publisherEnabled;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.backoffBaseMs = backoffBaseMs;
    }

    public List<OutboxEventRecord> publicarPendentes() {
        if (!publisherEnabled) {
            return publicarEventosPendentes.executar().join();
        }
        var publicados = new ArrayList<OutboxEventRecord>();
        for (var event : gateway.listarEventosPendentesParaPublicacao(batchSize)) {
            try {
                publicar(event);
                publicados.add(gateway.marcarEventoPublicado(event.eventId()));
            } catch (RuntimeException exception) {
                var attempts = event.attempts() + 1;
                var failed = attempts >= maxAttempts;
                gateway.marcarFalhaPublicacao(
                        event.eventId(),
                        rootMessage(exception),
                        nextAttempt(attempts),
                        failed);
                LOG.warnf(exception, "Falha ao publicar evento %s no topico %s", event.eventId(), event.topic());
            }
        }
        return List.copyOf(publicados);
    }

    private void publicar(OutboxEventRecord event) {
        if (!DomainMessagingRoutes.isProduced(event.eventType(), event.topic())) {
            throw new IllegalArgumentException("Evento nao produzido pelo oficina-os-service: " + event.eventType());
        }
        messagingClient.publish(event.topic(), codec.encode(event), Map.of(
                "eventType", event.eventType(),
                "eventVersion", Integer.toString(event.eventVersion()),
                "producer", event.producer(),
                "aggregateId", event.aggregateId().toString(),
                "correlationId", event.correlationId() == null ? event.eventId().toString() : event.correlationId()));
    }

    private OffsetDateTime nextAttempt(int attempts) {
        var multiplier = 1L << Math.min(10, Math.max(0, attempts - 1));
        return OffsetDateTime.now(ZoneOffset.UTC).plusNanos(backoffBaseMs * multiplier * 1_000_000L);
    }

    private static String rootMessage(Throwable throwable) {
        var current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}

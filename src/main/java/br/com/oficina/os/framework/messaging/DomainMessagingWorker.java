package br.com.oficina.os.framework.messaging;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
@Startup
class DomainMessagingWorker {
    private static final Logger LOG = Logger.getLogger(DomainMessagingWorker.class);

    private final OutboxPublisher outboxPublisher;
    private final SqsDomainEventConsumer sqsConsumer;
    private final boolean workerEnabled;
    private final long pollIntervalMs;
    private ScheduledExecutorService publisherExecutor;
    private ScheduledExecutorService consumerExecutor;

    DomainMessagingWorker(
            OutboxPublisher outboxPublisher,
            SqsDomainEventConsumer sqsConsumer,
            @ConfigProperty(name = "oficina.messaging.worker.enabled", defaultValue = "false") boolean workerEnabled,
            @ConfigProperty(name = "oficina.messaging.poll-interval-ms", defaultValue = "5000") long pollIntervalMs) {
        this.outboxPublisher = outboxPublisher;
        this.sqsConsumer = sqsConsumer;
        this.workerEnabled = workerEnabled;
        this.pollIntervalMs = pollIntervalMs;
    }

    @PostConstruct
    void start() {
        if (!workerEnabled) {
            return;
        }
        publisherExecutor = executor("outbox-publisher-worker");
        consumerExecutor = executor("domain-event-consumer-worker");
        publisherExecutor.scheduleWithFixedDelay(this::publishTick, 1000, Math.max(1000, pollIntervalMs), TimeUnit.MILLISECONDS);
        consumerExecutor.scheduleWithFixedDelay(this::consumeTick, 1000, Math.max(1000, pollIntervalMs), TimeUnit.MILLISECONDS);
    }

    private ScheduledExecutorService executor(String threadName) {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    void stop() {
        if (publisherExecutor != null) {
            publisherExecutor.shutdownNow();
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }

    private void publishTick() {
        try {
            outboxPublisher.publicarPendentes();
        } catch (RuntimeException exception) {
            LOG.warn("Falha no ciclo de publicacao da Outbox.", exception);
        }
    }

    private void consumeTick() {
        try {
            sqsConsumer.consumirDisponiveis();
        } catch (RuntimeException exception) {
            LOG.warn("Falha no ciclo de consumo de eventos de dominio.", exception);
        }
    }
}

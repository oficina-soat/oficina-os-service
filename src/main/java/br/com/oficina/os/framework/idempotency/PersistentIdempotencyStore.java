package br.com.oficina.os.framework.idempotency;

import br.com.oficina.os.framework.idempotency.IdempotencyRecord.ProcessingStatus;
import br.com.oficina.os.framework.observability.OperationalMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Optional;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PersistentIdempotencyStore implements IdempotencyStore {
    private final IdempotencyStore delegate;
    private final OperationalMetrics metrics;
    private final String database;

    @Inject
    public PersistentIdempotencyStore(
            @ConfigProperty(name = "oficina.persistence.kind", defaultValue = "postgresql") String persistenceKind,
            Instance<DataSource> dataSources,
            OperationalMetrics metrics) {
        this.delegate = createDelegate(persistenceKind, dataSources);
        this.metrics = metrics;
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }

    public PersistentIdempotencyStore(DataSource dataSource) {
        this.delegate = new PostgresIdempotencyStore(dataSource);
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), "oficina-os-service");
        this.database = "postgresql";
    }

    public PersistentIdempotencyStore(String persistenceKind, Instance<DataSource> dataSources) {
        this(
                persistenceKind,
                dataSources,
                new OperationalMetrics(new SimpleMeterRegistry(), "oficina-os-service"));
    }

    private IdempotencyStore createDelegate(String persistenceKind, Instance<DataSource> dataSources) {
        if ("memory".equalsIgnoreCase(persistenceKind)) {
            return new InMemoryIdempotencyStore();
        }
        if ("postgresql".equalsIgnoreCase(persistenceKind)) {
            return new PostgresIdempotencyStore(dataSources.get());
        }
        throw new IllegalArgumentException("oficina.persistence.kind deve ser postgresql ou memory: " + persistenceKind);
    }

    @Override
    public Optional<IdempotencyRecord> find(String scope, String key) {
        return metrics.persistence(database, "idempotency", "find", () -> delegate.find(scope, key));
    }

    @Override
    public IdempotencyRecord createProcessing(
            String scope,
            String key,
            String requestHash,
            String correlationId,
            String requestId,
            OffsetDateTime expiresAt) {
        return metrics.persistence(
                database,
                "idempotency",
                "create_processing",
                () -> delegate.createProcessing(scope, key, requestHash, correlationId, requestId, expiresAt));
    }

    @Override
    public void complete(
            String scope,
            String key,
            ProcessingStatus processingStatus,
            int responseStatus,
            String responseBody) {
        metrics.persistence(
                database,
                "idempotency",
                "complete",
                () -> delegate.complete(scope, key, processingStatus, responseStatus, responseBody));
    }
}

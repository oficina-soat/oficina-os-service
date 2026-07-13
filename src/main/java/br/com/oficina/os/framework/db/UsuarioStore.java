package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
import br.com.oficina.os.framework.observability.OperationalMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UsuarioStore implements UsuarioGateway {
    public static final UUID SEED_ADMIN_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    public static final UUID SEED_MECANICO_ID = UUID.fromString("20000000-0000-4000-8000-000000000002");
    public static final UUID SEED_RECEPCIONISTA_ID = UUID.fromString("20000000-0000-4000-8000-000000000003");

    private final UsuarioGateway delegate;
    private final OperationalMetrics metrics;
    private final String database;

    @Inject
    public UsuarioStore(
            @ConfigProperty(name = "oficina.persistence.kind", defaultValue = "postgresql") String persistenceKind,
            Instance<DataSource> dataSources,
            OperationalMetrics metrics) {
        this.delegate = createDelegate(persistenceKind, dataSources);
        this.metrics = metrics;
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }

    public UsuarioStore() {
        this.delegate = new InMemoryUsuarioGateway();
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), "oficina-os-service");
        this.database = "memory";
    }

    UsuarioStore(DataSource dataSource, String persistenceKind) {
        this.delegate = createDelegate(persistenceKind, dataSource);
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), "oficina-os-service");
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }

    private static UsuarioGateway createDelegate(String persistenceKind, Instance<DataSource> dataSources) {
        if ("memory".equalsIgnoreCase(persistenceKind)) {
            return new InMemoryUsuarioGateway();
        }
        if ("postgresql".equalsIgnoreCase(persistenceKind)) {
            return new PostgresUsuarioGateway(dataSources.get());
        }
        throw unsupportedPersistenceKind(persistenceKind);
    }

    private static UsuarioGateway createDelegate(String persistenceKind, DataSource dataSource) {
        if ("memory".equalsIgnoreCase(persistenceKind)) {
            return new InMemoryUsuarioGateway();
        }
        if ("postgresql".equalsIgnoreCase(persistenceKind)) {
            return new PostgresUsuarioGateway(dataSource);
        }
        throw unsupportedPersistenceKind(persistenceKind);
    }

    private static IllegalArgumentException unsupportedPersistenceKind(String persistenceKind) {
        return new IllegalArgumentException("oficina.persistence.kind deve ser postgresql ou memory: " + persistenceKind);
    }

    @Override
    public Usuario criar(Usuario usuario) {
        return metrics.persistence(database, "usuario", "create", () -> delegate.criar(usuario));
    }

    @Override
    public List<Usuario> listar() {
        return metrics.persistence(database, "usuario", "list", delegate::listar);
    }

    @Override
    public Usuario buscar(UUID usuarioId) {
        return metrics.persistence(database, "usuario", "find_by_id", () -> delegate.buscar(usuarioId));
    }

    @Override
    public Usuario atualizar(Usuario usuario) {
        return metrics.persistence(database, "usuario", "update", () -> delegate.atualizar(usuario));
    }

    @Override
    public void inativar(UUID usuarioId) {
        metrics.persistence(database, "usuario", "deactivate", () -> delegate.inativar(usuarioId));
    }
}

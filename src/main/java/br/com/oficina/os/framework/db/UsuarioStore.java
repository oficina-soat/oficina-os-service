package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.interfaces.gateway.UsuarioGateway;
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

    @Inject
    public UsuarioStore(
            @ConfigProperty(name = "oficina.persistence.kind", defaultValue = "postgresql") String persistenceKind,
            Instance<DataSource> dataSources) {
        this.delegate = createDelegate(persistenceKind, dataSources);
    }

    public UsuarioStore() {
        this.delegate = new InMemoryUsuarioGateway();
    }

    UsuarioStore(DataSource dataSource, String persistenceKind) {
        this.delegate = createDelegate(persistenceKind, dataSource);
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
        return delegate.criar(usuario);
    }

    @Override
    public List<Usuario> listar() {
        return delegate.listar();
    }

    @Override
    public Usuario buscar(UUID usuarioId) {
        return delegate.buscar(usuarioId);
    }

    @Override
    public Usuario atualizar(Usuario usuario) {
        return delegate.atualizar(usuario);
    }

    @Override
    public void inativar(UUID usuarioId) {
        delegate.inativar(usuarioId);
    }
}

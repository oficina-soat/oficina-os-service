package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import br.com.oficina.os.framework.observability.OperationalMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AtendimentoSeedStore implements AtendimentoGateway {
    public static final UUID SEED_CLIENTE_ID = AtendimentoGateway.SEED_CLIENTE_ID;
    public static final UUID SEED_VEICULO_ID = AtendimentoGateway.SEED_VEICULO_ID;
    public static final UUID SEED_ORDEM_SERVICO_ID = AtendimentoGateway.SEED_ORDEM_SERVICO_ID;

    private final AtendimentoGateway delegate;
    private final OperationalMetrics metrics;
    private final String database;

    @Inject
    public AtendimentoSeedStore(
            @ConfigProperty(name = "oficina.persistence.kind", defaultValue = "postgresql") String persistenceKind,
            Instance<DataSource> dataSources,
            OperationalMetrics metrics) {
        this.delegate = createDelegate(persistenceKind, dataSources);
        this.metrics = metrics;
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }

    public AtendimentoSeedStore() {
        this.delegate = new InMemoryAtendimentoGateway();
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), "oficina-os-service");
        this.database = "memory";
    }

    AtendimentoSeedStore(String persistenceKind, Instance<DataSource> dataSources) {
        this.delegate = createDelegate(persistenceKind, dataSources);
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), "oficina-os-service");
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }

    AtendimentoSeedStore(DataSource dataSource, String persistenceKind) {
        this.delegate = createDelegate(persistenceKind, dataSource);
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), "oficina-os-service");
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }

    private static AtendimentoGateway createDelegate(String persistenceKind, Instance<DataSource> dataSources) {
        if ("memory".equalsIgnoreCase(persistenceKind)) {
            return new InMemoryAtendimentoGateway();
        }
        if ("postgresql".equalsIgnoreCase(persistenceKind)) {
            return new PostgresAtendimentoGateway(dataSources.get());
        }
        throw unsupportedPersistenceKind(persistenceKind);
    }

    private static AtendimentoGateway createDelegate(String persistenceKind, DataSource dataSource) {
        if ("memory".equalsIgnoreCase(persistenceKind)) {
            return new InMemoryAtendimentoGateway();
        }
        if ("postgresql".equalsIgnoreCase(persistenceKind)) {
            return new PostgresAtendimentoGateway(dataSource);
        }
        throw unsupportedPersistenceKind(persistenceKind);
    }

    private static IllegalArgumentException unsupportedPersistenceKind(String persistenceKind) {
        return new IllegalArgumentException("oficina.persistence.kind deve ser postgresql ou memory: " + persistenceKind);
    }

    @Override
    public ClienteRecord criarCliente(String nome, String documento, String telefone, String email) {
        return persistence("cliente", "create", () -> delegate.criarCliente(nome, documento, telefone, email));
    }

    @Override
    public List<ClienteRecord> listarClientes() {
        return persistence("cliente", "list", delegate::listarClientes);
    }

    @Override
    public ClienteRecord buscarCliente(UUID clienteId) {
        return persistence("cliente", "find_by_id", () -> delegate.buscarCliente(clienteId));
    }

    @Override
    public ClienteRecord atualizarCliente(UUID clienteId, String nome, String documento, String telefone, String email) {
        return persistence("cliente", "update", () -> delegate.atualizarCliente(clienteId, nome, documento, telefone, email));
    }

    @Override
    public VeiculoRecord criarVeiculo(UUID clienteId, String placa, String marca, String modelo, int ano) {
        return persistence("veiculo", "create", () -> delegate.criarVeiculo(clienteId, placa, marca, modelo, ano));
    }

    @Override
    public List<VeiculoRecord> listarVeiculosDoCliente(UUID clienteId) {
        return persistence("veiculo", "list_by_cliente", () -> delegate.listarVeiculosDoCliente(clienteId));
    }

    @Override
    public VeiculoRecord buscarVeiculo(UUID veiculoId) {
        return persistence("veiculo", "find_by_id", () -> delegate.buscarVeiculo(veiculoId));
    }

    @Override
    public VeiculoRecord atualizarVeiculo(UUID veiculoId, String placa, String marca, String modelo, int ano) {
        return persistence("veiculo", "update", () -> delegate.atualizarVeiculo(veiculoId, placa, marca, modelo, ano));
    }

    @Override
    public OrdemServicoRecord criarOrdemServico(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        return persistence("ordem_servico", "create", () -> delegate.criarOrdemServico(clienteId, veiculoId, descricaoProblema));
    }

    @Override
    public List<OrdemServicoRecord> listarOrdensServico(TipoDeEstadoDaOrdemDeServico estado) {
        return persistence("ordem_servico", "list", () -> delegate.listarOrdensServico(estado));
    }

    @Override
    public OrdemServicoRecord buscarOrdemServico(UUID ordemServicoId) {
        return persistence("ordem_servico", "find_by_id", () -> delegate.buscarOrdemServico(ordemServicoId));
    }

    @Override
    public List<HistoricoRecord> historico(UUID ordemServicoId) {
        return persistence("ordem_servico_history", "list", () -> delegate.historico(ordemServicoId));
    }

    @Override
    public OrdemServicoRecord alterarEstado(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        return persistence("ordem_servico", "update_status", () -> delegate.alterarEstado(ordemServicoId, novoEstado, motivo));
    }

    @Override
    public OperacaoAssincronaRecord cancelar(UUID ordemServicoId, String motivo) {
        return persistence("ordem_servico", "cancel", () -> delegate.cancelar(ordemServicoId, motivo));
    }

    @Override
    public SagaRecord buscarSaga(UUID ordemServicoId) {
        return persistence("saga", "find_by_ordem", () -> delegate.buscarSaga(ordemServicoId));
    }

    @Override
    public List<SagaHistoricoRecord> historicoSaga(UUID ordemServicoId) {
        return persistence("saga_history", "list", () -> delegate.historicoSaga(ordemServicoId));
    }

    @Override
    public List<OutboxEventRecord> listarOutbox() {
        return persistence("outbox", "list", delegate::listarOutbox);
    }

    @Override
    public List<OutboxEventRecord> publicarEventosPendentes() {
        return persistence("outbox", "publish_pending_local", delegate::publicarEventosPendentes);
    }

    @Override
    public List<OutboxEventRecord> listarEventosPendentesParaPublicacao(int limit) {
        return persistence("outbox", "list_pending", () -> delegate.listarEventosPendentesParaPublicacao(limit));
    }

    @Override
    public OutboxEventRecord marcarEventoPublicado(UUID eventId) {
        return persistence("outbox", "mark_published", () -> delegate.marcarEventoPublicado(eventId));
    }

    @Override
    public OutboxEventRecord marcarFalhaPublicacao(UUID eventId, String lastError, OffsetDateTime nextAttemptAt, boolean failed) {
        return persistence("outbox", "mark_failure", () -> delegate.marcarFalhaPublicacao(eventId, lastError, nextAttemptAt, failed));
    }

    @Override
    public SagaRecord consumirEvento(DomainEventEnvelope event) {
        return persistence("consumed_event", "consume", () -> delegate.consumirEvento(event));
    }

    private <T> T persistence(String resource, String operation, java.util.function.Supplier<T> action) {
        return metrics.persistence(database, resource, operation, action);
    }
}

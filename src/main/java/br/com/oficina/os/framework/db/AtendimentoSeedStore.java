package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import br.com.oficina.os.framework.observability.OperationalMetrics;
import br.com.oficina.os.framework.observability.SagaObservability;
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
    private static final String SERVICE_NAME = "oficina-os-service";
    private static final String MEMORY = "memory", CREATE = "create", FIND_BY_ID = "find_by_id";
    private static final String CLIENTE = "cliente", VEICULO = "veiculo", ORDEM_SERVICO = "ordem_servico", OUTBOX = "outbox";
    public static final UUID SEED_CLIENTE_ID = AtendimentoGateway.SEED_CLIENTE_ID;
    public static final UUID SEED_VEICULO_ID = AtendimentoGateway.SEED_VEICULO_ID;
    public static final UUID SEED_ORDEM_SERVICO_ID = AtendimentoGateway.SEED_ORDEM_SERVICO_ID;
    private final AtendimentoGateway delegate;
    private final OperationalMetrics metrics;
    private final SagaObservability sagaObservability;
    private final String database;
    @Inject
    public AtendimentoSeedStore(
            @ConfigProperty(name = "oficina.persistence.kind", defaultValue = "postgresql") String persistenceKind,
            Instance<DataSource> dataSources,
            OperationalMetrics metrics,
            SagaObservability sagaObservability) {
        this.delegate = createDelegate(persistenceKind, dataSources);
        this.metrics = metrics;
        this.sagaObservability = sagaObservability;
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }
    public AtendimentoSeedStore() {
        this.delegate = new InMemoryAtendimentoGateway();
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), SERVICE_NAME);
        this.sagaObservability = new SagaObservability(metrics);
        this.database = MEMORY;
    }

    AtendimentoSeedStore(String persistenceKind, Instance<DataSource> dataSources) {
        this.delegate = createDelegate(persistenceKind, dataSources);
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), SERVICE_NAME);
        this.sagaObservability = new SagaObservability(metrics);
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }

    AtendimentoSeedStore(DataSource dataSource, String persistenceKind) {
        this.delegate = createDelegate(persistenceKind, dataSource);
        this.metrics = new OperationalMetrics(new SimpleMeterRegistry(), SERVICE_NAME);
        this.sagaObservability = new SagaObservability(metrics);
        this.database = persistenceKind.toLowerCase(java.util.Locale.ROOT);
    }

    AtendimentoSeedStore(AtendimentoGateway delegate, OperationalMetrics metrics, String database) {
        this.delegate = delegate;
        this.metrics = metrics;
        this.sagaObservability = new SagaObservability(metrics);
        this.database = database;
    }

    private static AtendimentoGateway createDelegate(String persistenceKind, Instance<DataSource> dataSources) {
        if (MEMORY.equalsIgnoreCase(persistenceKind)) {
            return new InMemoryAtendimentoGateway();
        }
        if ("postgresql".equalsIgnoreCase(persistenceKind)) {
            return new PostgresAtendimentoGateway(dataSources.get());
        }
        throw unsupportedPersistenceKind(persistenceKind);
    }

    private static AtendimentoGateway createDelegate(String persistenceKind, DataSource dataSource) {
        if (MEMORY.equalsIgnoreCase(persistenceKind)) {
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
        return persistence(CLIENTE, CREATE, () -> delegate.criarCliente(nome, documento, telefone, email));
    }

    @Override
    public List<ClienteRecord> listarClientes(ClienteSearchCriteria criteria) {
        return persistence(CLIENTE, "list", () -> delegate.listarClientes(criteria));
    }

    @Override
    public ClienteRecord buscarCliente(UUID clienteId) {
        return persistence(CLIENTE, FIND_BY_ID, () -> delegate.buscarCliente(clienteId));
    }

    @Override
    public ClienteRecord atualizarCliente(UUID clienteId, String nome, String documento, String telefone, String email) {
        return persistence(CLIENTE, "update", () -> delegate.atualizarCliente(clienteId, nome, documento, telefone, email));
    }

    @Override
    public VeiculoRecord criarVeiculo(UUID clienteId, String placa, String marca, String modelo, int ano) {
        return persistence(VEICULO, CREATE, () -> delegate.criarVeiculo(clienteId, placa, marca, modelo, ano));
    }

    @Override
    public List<VeiculoRecord> listarVeiculosDoCliente(UUID clienteId) {
        return persistence(VEICULO, "list_by_cliente", () -> delegate.listarVeiculosDoCliente(clienteId));
    }

    @Override
    public VeiculoRecord buscarVeiculo(UUID veiculoId) {
        return persistence(VEICULO, FIND_BY_ID, () -> delegate.buscarVeiculo(veiculoId));
    }

    @Override
    public VeiculoRecord atualizarVeiculo(UUID veiculoId, String placa, String marca, String modelo, int ano) {
        return persistence(VEICULO, "update", () -> delegate.atualizarVeiculo(veiculoId, placa, marca, modelo, ano));
    }

    @Override
    public OrdemServicoRecord criarOrdemServico(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        var ordem = persistence(ORDEM_SERVICO, CREATE, () -> delegate.criarOrdemServico(clienteId, veiculoId, descricaoProblema));
        sagaObservability.observe(null, delegate.buscarSaga(ordem.ordemServicoId()));
        return ordem;
    }

    @Override
    public List<OrdemServicoRecord> listarOrdensServico(TipoDeEstadoDaOrdemDeServico estado) {
        return persistence(ORDEM_SERVICO, "list", () -> delegate.listarOrdensServico(estado));
    }

    @Override
    public OrdemServicoRecord buscarOrdemServico(UUID ordemServicoId) {
        return persistence(ORDEM_SERVICO, FIND_BY_ID, () -> delegate.buscarOrdemServico(ordemServicoId));
    }

    @Override
    public OrdemServicoRecord incluirServico(UUID id, ItemServicoRecord item, String correlationId) {
        return persistence(ORDEM_SERVICO, "add_service", () -> delegate.incluirServico(id, item, correlationId));
    }

    @Override
    public OrdemServicoRecord incluirPeca(UUID id, ItemPecaRecord item, String correlationId) {
        return persistence(ORDEM_SERVICO, "add_part", () -> delegate.incluirPeca(id, item, correlationId));
    }
    @Override
    public List<HistoricoRecord> historico(UUID ordemServicoId) {
        return persistence("ordem_servico_history", "list", () -> delegate.historico(ordemServicoId));
    }

    @Override
    public OrdemServicoRecord alterarEstado(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        var previous = delegate.buscarSaga(ordemServicoId);
        var ordem = persistence(ORDEM_SERVICO, "update_status", () -> delegate.alterarEstado(ordemServicoId, novoEstado, motivo));
        sagaObservability.observe(previous, delegate.buscarSaga(ordemServicoId));
        return ordem;
    }

    @Override
    public OperacaoAssincronaRecord cancelar(UUID ordemServicoId, String motivo) {
        var previous = delegate.buscarSaga(ordemServicoId);
        var operacao = persistence(ORDEM_SERVICO, "cancel", () -> delegate.cancelar(ordemServicoId, motivo));
        sagaObservability.observe(previous, delegate.buscarSaga(ordemServicoId));
        return operacao;
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
    public List<OutboxEventRecord> listarOutbox() { return persistence(OUTBOX, "list", delegate::listarOutbox); }
    @Override
    public List<OutboxEventRecord> publicarEventosPendentes() { return persistence(OUTBOX, "publish_pending_local", delegate::publicarEventosPendentes); }
    @Override
    public List<OutboxEventRecord> listarEventosPendentesParaPublicacao(int limit) { return delegate.listarEventosPendentesParaPublicacao(limit); }

    @Override
    public List<OutboxEventRecord> reivindicarEventosPendentes(
            int limit, String claimOwner, OffsetDateTime claimUntil) {
        return persistence(OUTBOX, "claim", () -> delegate.reivindicarEventosPendentes(limit, claimOwner, claimUntil));
    }
    @Override
    public OutboxEventRecord marcarEventoPublicado(UUID eventId) { return delegate.marcarEventoPublicado(eventId); }

    @Override
    public OutboxEventRecord marcarEventoPublicado(UUID eventId, String owner) { return delegate.marcarEventoPublicado(eventId, owner); }
    @Override
    public OutboxEventRecord marcarFalhaPublicacao(UUID eventId, String lastError, OffsetDateTime nextAttemptAt, boolean failed) {
        return persistence(OUTBOX, "mark_failure", () -> delegate.marcarFalhaPublicacao(eventId, lastError, nextAttemptAt, failed));
    }

    @Override
    public OutboxEventRecord marcarFalhaPublicacao(UUID eventId, String error, OffsetDateTime next, boolean failed, String owner) {
        return persistence(OUTBOX, "mark_failure", () -> delegate.marcarFalhaPublicacao(eventId, error, next, failed, owner));
    }
    @Override
    public SagaRecord consumirEvento(DomainEventEnvelope event) {
        var ordemServicoId = AtendimentoGatewaySupport.uuidFromPayload(
                event.payload(), AtendimentoGatewaySupport.PAYLOAD_ORDEM_SERVICO_ID, event.aggregateId());
        var previous = delegate.buscarSaga(ordemServicoId);
        var current = persistence("consumed_event", "consume", () -> delegate.consumirEvento(event));
        sagaObservability.observe(previous, current);
        return current;
    }
    private <T> T persistence(String resource, String operation, java.util.function.Supplier<T> action) {
        return metrics.persistence(database, resource, operation, action);
    }
}

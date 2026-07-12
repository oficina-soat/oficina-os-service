package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
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

    @Inject
    public AtendimentoSeedStore(
            @ConfigProperty(name = "oficina.persistence.kind", defaultValue = "postgresql") String persistenceKind,
            Instance<DataSource> dataSources) {
        this.delegate = createDelegate(persistenceKind, dataSources);
    }

    public AtendimentoSeedStore() {
        this.delegate = new InMemoryAtendimentoGateway();
    }

    AtendimentoSeedStore(DataSource dataSource, String persistenceKind) {
        this.delegate = createDelegate(persistenceKind, dataSource);
    }

    private static AtendimentoGateway createDelegate(String persistenceKind, Instance<DataSource> dataSources) {
        if ("memory".equalsIgnoreCase(persistenceKind)) {
            return new InMemoryAtendimentoGateway();
        }
        return new PostgresAtendimentoGateway(dataSources.get());
    }

    private static AtendimentoGateway createDelegate(String persistenceKind, DataSource dataSource) {
        if ("memory".equalsIgnoreCase(persistenceKind)) {
            return new InMemoryAtendimentoGateway();
        }
        return new PostgresAtendimentoGateway(dataSource);
    }

    @Override
    public ClienteRecord criarCliente(String nome, String documento, String telefone, String email) {
        return delegate.criarCliente(nome, documento, telefone, email);
    }

    @Override
    public List<ClienteRecord> listarClientes() {
        return delegate.listarClientes();
    }

    @Override
    public ClienteRecord buscarCliente(UUID clienteId) {
        return delegate.buscarCliente(clienteId);
    }

    @Override
    public ClienteRecord atualizarCliente(UUID clienteId, String nome, String documento, String telefone, String email) {
        return delegate.atualizarCliente(clienteId, nome, documento, telefone, email);
    }

    @Override
    public VeiculoRecord criarVeiculo(UUID clienteId, String placa, String marca, String modelo, int ano) {
        return delegate.criarVeiculo(clienteId, placa, marca, modelo, ano);
    }

    @Override
    public List<VeiculoRecord> listarVeiculosDoCliente(UUID clienteId) {
        return delegate.listarVeiculosDoCliente(clienteId);
    }

    @Override
    public VeiculoRecord buscarVeiculo(UUID veiculoId) {
        return delegate.buscarVeiculo(veiculoId);
    }

    @Override
    public VeiculoRecord atualizarVeiculo(UUID veiculoId, String placa, String marca, String modelo, int ano) {
        return delegate.atualizarVeiculo(veiculoId, placa, marca, modelo, ano);
    }

    @Override
    public OrdemServicoRecord criarOrdemServico(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        return delegate.criarOrdemServico(clienteId, veiculoId, descricaoProblema);
    }

    @Override
    public List<OrdemServicoRecord> listarOrdensServico(TipoDeEstadoDaOrdemDeServico estado) {
        return delegate.listarOrdensServico(estado);
    }

    @Override
    public OrdemServicoRecord buscarOrdemServico(UUID ordemServicoId) {
        return delegate.buscarOrdemServico(ordemServicoId);
    }

    @Override
    public List<HistoricoRecord> historico(UUID ordemServicoId) {
        return delegate.historico(ordemServicoId);
    }

    @Override
    public OrdemServicoRecord alterarEstado(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        return delegate.alterarEstado(ordemServicoId, novoEstado, motivo);
    }

    @Override
    public OperacaoAssincronaRecord cancelar(UUID ordemServicoId, String motivo) {
        return delegate.cancelar(ordemServicoId, motivo);
    }

    @Override
    public SagaRecord buscarSaga(UUID ordemServicoId) {
        return delegate.buscarSaga(ordemServicoId);
    }

    @Override
    public List<SagaHistoricoRecord> historicoSaga(UUID ordemServicoId) {
        return delegate.historicoSaga(ordemServicoId);
    }

    @Override
    public List<OutboxEventRecord> listarOutbox() {
        return delegate.listarOutbox();
    }

    @Override
    public List<OutboxEventRecord> publicarEventosPendentes() {
        return delegate.publicarEventosPendentes();
    }

    @Override
    public List<OutboxEventRecord> listarEventosPendentesParaPublicacao(int limit) {
        return delegate.listarEventosPendentesParaPublicacao(limit);
    }

    @Override
    public OutboxEventRecord marcarEventoPublicado(UUID eventId) {
        return delegate.marcarEventoPublicado(eventId);
    }

    @Override
    public OutboxEventRecord marcarFalhaPublicacao(UUID eventId, String lastError, OffsetDateTime nextAttemptAt, boolean failed) {
        return delegate.marcarFalhaPublicacao(eventId, lastError, nextAttemptAt, failed);
    }

    @Override
    public SagaRecord consumirEvento(DomainEventEnvelope event) {
        return delegate.consumirEvento(event);
    }
}

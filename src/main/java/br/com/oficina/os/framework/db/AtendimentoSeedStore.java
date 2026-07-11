package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.cliente.DocumentoFactory;
import br.com.oficina.os.core.entities.cliente.Email;
import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.entities.veiculo.MarcaDeVeiculo;
import br.com.oficina.os.core.entities.veiculo.ModeloDeVeiculo;
import br.com.oficina.os.core.entities.veiculo.PlacaDeVeiculo;
import br.com.oficina.os.framework.messaging.DomainEventEnvelope;
import br.com.oficina.os.framework.messaging.OutboxEventRecord;
import br.com.oficina.os.framework.observability.StructuredLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@ApplicationScoped
public class AtendimentoSeedStore {
    private static final Logger LOG = Logger.getLogger(AtendimentoSeedStore.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, Object>> JSON_MAP = new TypeReference<>() {
    };
    public static final UUID SEED_CLIENTE_ID = UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851");
    public static final UUID SEED_VEICULO_ID = UUID.fromString("7b1f1a8d-7f4a-4f25-8e74-27d50210a61e");
    public static final UUID SEED_ORDEM_SERVICO_ID = UUID.fromString("f05dd17b-daae-4658-af7c-363dd6e6fdfb");
    private static final String PRODUCER = "oficina-os-service";
    private static final String EVENT_ORDEM_DE_SERVICO_CRIADA = "ordemDeServicoCriada";
    private static final String EVENT_SAGA_COMPENSADA = "sagaCompensada";
    private static final String PAYLOAD_ORDEM_SERVICO_ID = "ordemServicoId";
    private static final String PAYLOAD_ESTADO_ATUAL = "estadoAtual";
    private static final String PAYLOAD_EXECUCAO_ID = "execucaoId";
    private static final String PAYLOAD_ORCAMENTO_ID = "orcamentoId";
    private static final String PAYLOAD_PAGAMENTO_ID = "pagamentoId";
    private static final String PAYLOAD_MOTIVO = "motivo";

    private final LinkedHashMap<UUID, ClienteRecord> clientes = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, VeiculoRecord> veiculos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, OrdemServicoRecord> ordensServico = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, List<HistoricoRecord>> historicos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, SagaRecord> sagasByOrdemServico = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, List<SagaHistoricoRecord>> sagaHistoricos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, OutboxEventRecord> outboxEvents = new LinkedHashMap<>();
    private final Set<UUID> consumedEventIds = new LinkedHashSet<>();
    private final DataSource dataSource;
    private final boolean postgresql;

    @Inject
    public AtendimentoSeedStore(
            @ConfigProperty(name = "oficina.persistence.kind", defaultValue = "postgresql") String persistenceKind,
            Instance<DataSource> dataSources) {
        this.postgresql = !"memory".equalsIgnoreCase(persistenceKind);
        this.dataSource = postgresql ? dataSources.get() : null;
        if (!postgresql) {
            seedMemory();
        }
    }

    public AtendimentoSeedStore() {
        this.dataSource = null;
        this.postgresql = false;
        seedMemory();
    }

    AtendimentoSeedStore(DataSource dataSource, String persistenceKind) {
        this.postgresql = !"memory".equalsIgnoreCase(persistenceKind);
        this.dataSource = postgresql ? dataSource : null;
        if (!postgresql) {
            seedMemory();
        }
    }

    private void seedMemory() {
        var seedTime = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);
        clientes.put(SEED_CLIENTE_ID, new ClienteRecord(
                SEED_CLIENTE_ID,
                "Maria Souza",
                "12345678901",
                "+5511999999999",
                "maria@example.com",
                seedTime,
                seedTime));
        veiculos.put(SEED_VEICULO_ID, new VeiculoRecord(
                SEED_VEICULO_ID,
                SEED_CLIENTE_ID,
                "ABC1D23",
                "Volkswagen",
                "Gol",
                2020,
                seedTime,
                seedTime));
        ordensServico.put(SEED_ORDEM_SERVICO_ID, new OrdemServicoRecord(
                SEED_ORDEM_SERVICO_ID,
                SEED_CLIENTE_ID,
                SEED_VEICULO_ID,
                "Veiculo nao liga",
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                seedTime,
                seedTime));
        historicos.put(SEED_ORDEM_SERVICO_ID, new ArrayList<>(List.of(new HistoricoRecord(
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                seedTime,
                "Ordem de servico recebida"))));
        criarSagaInicial(SEED_ORDEM_SERVICO_ID, seedTime, "seed");
    }

    public synchronized ClienteRecord criarCliente(String nome, String documento, String telefone, String email) {
        if (postgresql) {
            return criarClientePostgres(nome, documento, telefone, email);
        }
        validarCliente(nome, documento, email);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var cliente = new ClienteRecord(UUID.randomUUID(), nome.trim(), documento.trim(), normalizar(telefone), normalizar(email), agora, agora);
        clientes.put(cliente.clienteId(), cliente);
        return cliente;
    }

    public synchronized List<ClienteRecord> listarClientes() {
        if (postgresql) {
            return listarClientesPostgres();
        }
        return clientes.values().stream()
                .sorted(Comparator.comparing(ClienteRecord::criadoEm))
                .toList();
    }

    public synchronized ClienteRecord buscarCliente(UUID clienteId) {
        if (postgresql) {
            return buscarClientePostgres(clienteId);
        }
        var cliente = clientes.get(clienteId);
        if (cliente == null) {
            throw new NotFoundException("Cliente nao encontrado: " + clienteId);
        }
        return cliente;
    }

    public synchronized ClienteRecord atualizarCliente(UUID clienteId, String nome, String documento, String telefone, String email) {
        if (postgresql) {
            return atualizarClientePostgres(clienteId, nome, documento, telefone, email);
        }
        var atual = buscarCliente(clienteId);
        validarCliente(nome, documento, email);
        var atualizado = new ClienteRecord(
                atual.clienteId(),
                nome.trim(),
                documento.trim(),
                normalizar(telefone),
                normalizar(email),
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC));
        clientes.put(clienteId, atualizado);
        return atualizado;
    }

    public synchronized VeiculoRecord criarVeiculo(UUID clienteId, String placa, String marca, String modelo, int ano) {
        if (postgresql) {
            return criarVeiculoPostgres(clienteId, placa, marca, modelo, ano);
        }
        buscarCliente(clienteId);
        validarVeiculo(placa, marca, modelo, ano);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var veiculo = new VeiculoRecord(UUID.randomUUID(), clienteId, placa.trim().toUpperCase(), marca.trim(), modelo.trim(), ano, agora, agora);
        veiculos.put(veiculo.veiculoId(), veiculo);
        return veiculo;
    }

    public synchronized List<VeiculoRecord> listarVeiculosDoCliente(UUID clienteId) {
        if (postgresql) {
            return listarVeiculosDoClientePostgres(clienteId);
        }
        buscarCliente(clienteId);
        return veiculos.values().stream()
                .filter(veiculo -> veiculo.clienteId().equals(clienteId))
                .sorted(Comparator.comparing(VeiculoRecord::criadoEm))
                .toList();
    }

    public synchronized VeiculoRecord buscarVeiculo(UUID veiculoId) {
        if (postgresql) {
            return buscarVeiculoPostgres(veiculoId);
        }
        var veiculo = veiculos.get(veiculoId);
        if (veiculo == null) {
            throw new NotFoundException("Veiculo nao encontrado: " + veiculoId);
        }
        return veiculo;
    }

    public synchronized VeiculoRecord atualizarVeiculo(UUID veiculoId, String placa, String marca, String modelo, int ano) {
        if (postgresql) {
            return atualizarVeiculoPostgres(veiculoId, placa, marca, modelo, ano);
        }
        var atual = buscarVeiculo(veiculoId);
        validarVeiculo(placa, marca, modelo, ano);
        var atualizado = new VeiculoRecord(
                atual.veiculoId(),
                atual.clienteId(),
                placa.trim().toUpperCase(),
                marca.trim(),
                modelo.trim(),
                ano,
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC));
        veiculos.put(veiculoId, atualizado);
        return atualizado;
    }

    public synchronized OrdemServicoRecord criarOrdemServico(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        if (postgresql) {
            return criarOrdemServicoPostgres(clienteId, veiculoId, descricaoProblema);
        }
        buscarCliente(clienteId);
        var veiculo = buscarVeiculo(veiculoId);
        if (!veiculo.clienteId().equals(clienteId)) {
            throw new WebApplicationException("Veiculo nao pertence ao cliente informado.", Response.Status.CONFLICT);
        }
        if (descricaoProblema == null || descricaoProblema.isBlank()) {
            throw new IllegalArgumentException("Descricao do problema e obrigatoria.");
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var ordem = new OrdemServicoRecord(
                UUID.randomUUID(),
                clienteId,
                veiculoId,
                descricaoProblema.trim(),
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                agora,
                agora);
        ordensServico.put(ordem.ordemServicoId(), ordem);
        historicos.put(ordem.ordemServicoId(), new ArrayList<>(List.of(new HistoricoRecord(
                ordem.estado(),
                agora,
                "Ordem de servico recebida"))));
        var correlationId = correlationId(null);
        criarSagaInicial(ordem.ordemServicoId(), agora, correlationId);
        enfileirarEvento(
                EVENT_ORDEM_DE_SERVICO_CRIADA,
                "oficina.os.ordem-de-servico-criada",
                ordem.ordemServicoId(),
                Map.of(
                        PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString(),
                        "clienteId", clienteId.toString(),
                        "veiculoId", veiculoId.toString(),
                        PAYLOAD_ESTADO_ATUAL, ordem.estado().name(),
                        "criadoEm", agora.toString(),
                        "descricaoProblema", ordem.descricaoProblema()),
                correlationId,
                agora);
        return ordem;
    }

    public synchronized List<OrdemServicoRecord> listarOrdensServico(TipoDeEstadoDaOrdemDeServico estado) {
        if (postgresql) {
            return listarOrdensServicoPostgres(estado);
        }
        return ordensServico.values().stream()
                .filter(ordem -> estado == null || ordem.estado() == estado)
                .sorted(Comparator.comparing(OrdemServicoRecord::criadoEm))
                .toList();
    }

    public synchronized OrdemServicoRecord buscarOrdemServico(UUID ordemServicoId) {
        if (postgresql) {
            return buscarOrdemServicoPostgres(ordemServicoId);
        }
        var ordem = ordensServico.get(ordemServicoId);
        if (ordem == null) {
            throw new NotFoundException("Ordem de servico nao encontrada: " + ordemServicoId);
        }
        return ordem;
    }

    public synchronized List<HistoricoRecord> historico(UUID ordemServicoId) {
        if (postgresql) {
            return historicoPostgres(ordemServicoId);
        }
        buscarOrdemServico(ordemServicoId);
        return List.copyOf(historicos.getOrDefault(ordemServicoId, List.of()));
    }

    public synchronized OrdemServicoRecord alterarEstado(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        if (postgresql) {
            return alterarEstadoPostgres(ordemServicoId, novoEstado, motivo);
        }
        var atual = buscarOrdemServico(ordemServicoId);
        validarTransicao(atual.estado(), novoEstado);
        var atualizado = new OrdemServicoRecord(
                atual.ordemServicoId(),
                atual.clienteId(),
                atual.veiculoId(),
                atual.descricaoProblema(),
                novoEstado,
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC));
        ordensServico.put(ordemServicoId, atualizado);
        historicos.computeIfAbsent(ordemServicoId, _ -> new ArrayList<>())
                .add(new HistoricoRecord(novoEstado, atualizado.atualizadoEm(), normalizar(motivo)));
        if (novoEstado == TipoDeEstadoDaOrdemDeServico.ENTREGUE) {
            finalizarSagaComEntrega(atualizado, normalizar(motivo));
        }
        return atualizado;
    }

    public synchronized OperacaoAssincronaRecord cancelar(UUID ordemServicoId, String motivo) {
        if (postgresql) {
            return cancelarPostgres(ordemServicoId, motivo);
        }
        buscarOrdemServico(ordemServicoId);
        compensarSaga(ordemServicoId, normalizar(motivo));
        return new OperacaoAssincronaRecord("ACEITO", OffsetDateTime.now(ZoneOffset.UTC));
    }

    public synchronized SagaRecord buscarSaga(UUID ordemServicoId) {
        if (postgresql) {
            return buscarSagaPostgres(ordemServicoId);
        }
        buscarOrdemServico(ordemServicoId);
        return sagasByOrdemServico.get(ordemServicoId);
    }

    public synchronized List<SagaHistoricoRecord> historicoSaga(UUID ordemServicoId) {
        if (postgresql) {
            return historicoSagaPostgres(ordemServicoId);
        }
        var saga = buscarSaga(ordemServicoId);
        return saga == null ? List.of() : List.copyOf(sagaHistoricos.getOrDefault(saga.sagaId(), List.of()));
    }

    public synchronized List<OutboxEventRecord> listarOutbox() {
        if (postgresql) {
            return listarOutboxPostgres();
        }
        return List.copyOf(outboxEvents.values());
    }

    public synchronized List<OutboxEventRecord> publicarEventosPendentes() {
        if (postgresql) {
            return publicarEventosPendentesPostgres();
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var publicados = new ArrayList<OutboxEventRecord>();
        for (var event : new ArrayList<>(outboxEvents.values())) {
            if (!"PENDING".equals(event.status())) {
                continue;
            }
            var publicado = new OutboxEventRecord(
                    event.eventId(),
                    event.aggregateId(),
                    event.eventType(),
                    event.eventVersion(),
                    event.topic(),
                    event.producer(),
                    event.payload(),
                    "PUBLISHED",
                    event.correlationId(),
                    event.occurredAt(),
                    event.createdAt(),
                    agora,
                    event.attempts() + 1,
                    null);
            outboxEvents.put(publicado.eventId(), publicado);
            publicados.add(publicado);
            logEvent("outbox event published", publicado, "PUBLISHED");
        }
        return publicados;
    }

    public synchronized SagaRecord consumirEvento(DomainEventEnvelope event) {
        if (postgresql) {
            return consumirEventoPostgres(event);
        }
        if (consumedEventIds.contains(event.eventId())) {
            var saga = sagasByOrdemServico.get(event.aggregateId());
            logEvent("domain event ignored", event, "DUPLICATE", event.aggregateId(), correlationId(saga, event));
            return saga;
        }
        var ordemServicoId = uuidFromPayload(event.payload(), PAYLOAD_ORDEM_SERVICO_ID, event.aggregateId());
        var saga = buscarSaga(ordemServicoId);
        if (saga == null) {
            throw new NotFoundException("Saga da ordem de servico nao encontrada: " + ordemServicoId);
        }
        consumedEventIds.add(event.eventId());

        var resultado = switch (event.eventType()) {
            case "diagnosticoIniciado" -> processarDiagnosticoIniciado(saga, event);
            case "diagnosticoFinalizado" -> processarDiagnosticoFinalizado(saga, event);
            case "orcamentoGerado" -> transicionarSaga(saga, new SagaTransition(
                    EstadoSaga.AGUARDANDO_APROVACAO,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "orcamentoGerado",
                    null,
                    new SagaExternalIds(saga.execucaoId(), uuidFromPayload(event.payload(), PAYLOAD_ORCAMENTO_ID, saga.orcamentoId()), saga.pagamentoId()),
                    event.occurredAt(),
                    correlationId(saga, event)));
            case "orcamentoAprovado" -> transicionarSaga(saga, new SagaTransition(
                    EstadoSaga.EM_EXECUCAO,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "orcamentoAprovado",
                    null,
                    new SagaExternalIds(saga.execucaoId(), uuidFromPayload(event.payload(), PAYLOAD_ORCAMENTO_ID, saga.orcamentoId()), saga.pagamentoId()),
                    event.occurredAt(),
                    correlationId(saga, event)));
            case "orcamentoRecusado" -> processarOrcamentoRecusado(saga, event);
            case "execucaoIniciada" -> processarExecucaoIniciada(saga, event);
            case "execucaoFinalizada" -> processarExecucaoFinalizada(saga, event);
            case "pagamentoSolicitado" -> transicionarSaga(saga, new SagaTransition(
                    EstadoSaga.AGUARDANDO_PAGAMENTO,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "pagamentoSolicitado",
                    null,
                    new SagaExternalIds(
                            saga.execucaoId(),
                            uuidFromPayload(event.payload(), PAYLOAD_ORCAMENTO_ID, saga.orcamentoId()),
                            uuidFromPayload(event.payload(), PAYLOAD_PAGAMENTO_ID, saga.pagamentoId())),
                    event.occurredAt(),
                    correlationId(saga, event)));
            case "pagamentoConfirmado" -> transicionarSaga(saga, new SagaTransition(
                    EstadoSaga.AGUARDANDO_ENTREGA,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "pagamentoConfirmado",
                    null,
                    new SagaExternalIds(
                            saga.execucaoId(),
                            saga.orcamentoId(),
                            uuidFromPayload(event.payload(), PAYLOAD_PAGAMENTO_ID, saga.pagamentoId())),
                    event.occurredAt(),
                    correlationId(saga, event)));
            case "pagamentoRecusado" -> transicionarSaga(saga, new SagaTransition(
                    EstadoSaga.AGUARDANDO_PAGAMENTO,
                    buscarOrdemServico(ordemServicoId).estado(),
                    "pagamentoRecusado",
                    stringFromPayload(event.payload(), PAYLOAD_MOTIVO),
                    new SagaExternalIds(
                            saga.execucaoId(),
                            saga.orcamentoId(),
                            uuidFromPayload(event.payload(), PAYLOAD_PAGAMENTO_ID, saga.pagamentoId())),
                    event.occurredAt(),
                    correlationId(saga, event)));
            default -> saga;
        };
        logEvent("domain event consumed", event, "CONSUMED", ordemServicoId, correlationId(resultado, event));
        return resultado;
    }

    private ClienteRecord criarClientePostgres(String nome, String documento, String telefone, String email) {
        validarCliente(nome, documento, email);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var clienteId = UUID.randomUUID();
        var pessoaId = UUID.randomUUID();
        var documentoNormalizado = documento.trim();
        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement("""
                        INSERT INTO pessoa (id, documento, tipo_pessoa, nome, criado_em, atualizado_em)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setObject(1, pessoaId);
                    statement.setString(2, documentoNormalizado);
                    statement.setString(3, documentoNormalizado.length() == 14 ? "JURIDICA" : "FISICA");
                    statement.setString(4, nome.trim());
                    statement.setObject(5, agora);
                    statement.setObject(6, agora);
                    statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement("""
                        INSERT INTO cliente (id, pessoa_id, email, telefone, criado_em, atualizado_em)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setObject(1, clienteId);
                    statement.setObject(2, pessoaId);
                    statement.setString(3, normalizar(email));
                    statement.setString(4, normalizar(telefone));
                    statement.setObject(5, agora);
                    statement.setObject(6, agora);
                    statement.executeUpdate();
                }
                connection.commit();
                return new ClienteRecord(clienteId, nome.trim(), documentoNormalizado, normalizar(telefone), normalizar(email), agora, agora);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private List<ClienteRecord> listarClientesPostgres() {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        SELECT c.id AS cliente_id, p.nome, p.documento, c.telefone, c.email, c.criado_em, c.atualizado_em
                        FROM cliente c
                        JOIN pessoa p ON p.id = c.pessoa_id
                        ORDER BY c.criado_em
                        """);
                var resultSet = statement.executeQuery()) {
            var result = new ArrayList<ClienteRecord>();
            while (resultSet.next()) {
                result.add(toCliente(resultSet));
            }
            return List.copyOf(result);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private ClienteRecord buscarClientePostgres(UUID clienteId) {
        try (var connection = dataSource.getConnection()) {
            return buscarClientePostgres(connection, clienteId);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private ClienteRecord atualizarClientePostgres(UUID clienteId, String nome, String documento, String telefone, String email) {
        validarCliente(nome, documento, email);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                var pessoaId = pessoaIdDoCliente(connection, clienteId);
                try (var statement = connection.prepareStatement("""
                        UPDATE pessoa
                        SET nome = ?, documento = ?, tipo_pessoa = ?, atualizado_em = ?
                        WHERE id = ?
                        """)) {
                    var documentoNormalizado = documento.trim();
                    statement.setString(1, nome.trim());
                    statement.setString(2, documentoNormalizado);
                    statement.setString(3, documentoNormalizado.length() == 14 ? "JURIDICA" : "FISICA");
                    statement.setObject(4, agora);
                    statement.setObject(5, pessoaId);
                    statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement("""
                        UPDATE cliente
                        SET email = ?, telefone = ?, atualizado_em = ?
                        WHERE id = ?
                        """)) {
                    statement.setString(1, normalizar(email));
                    statement.setString(2, normalizar(telefone));
                    statement.setObject(3, agora);
                    statement.setObject(4, clienteId);
                    statement.executeUpdate();
                }
                connection.commit();
                return buscarClientePostgres(clienteId);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private VeiculoRecord criarVeiculoPostgres(UUID clienteId, String placa, String marca, String modelo, int ano) {
        buscarClientePostgres(clienteId);
        validarVeiculo(placa, marca, modelo, ano);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var veiculo = new VeiculoRecord(UUID.randomUUID(), clienteId, placa.trim().toUpperCase(), marca.trim(), modelo.trim(), ano, agora, agora);
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        INSERT INTO veiculo (id, cliente_id, placa, marca, modelo, ano, criado_em, atualizado_em)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
            statement.setObject(1, veiculo.veiculoId());
            statement.setObject(2, clienteId);
            statement.setString(3, veiculo.placa());
            statement.setString(4, veiculo.marca());
            statement.setString(5, veiculo.modelo());
            statement.setInt(6, ano);
            statement.setObject(7, agora);
            statement.setObject(8, agora);
            statement.executeUpdate();
            return veiculo;
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private List<VeiculoRecord> listarVeiculosDoClientePostgres(UUID clienteId) {
        buscarClientePostgres(clienteId);
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        SELECT id, cliente_id, placa, marca, modelo, ano, criado_em, atualizado_em
                        FROM veiculo
                        WHERE cliente_id = ?
                        ORDER BY criado_em
                        """)) {
            statement.setObject(1, clienteId);
            try (var resultSet = statement.executeQuery()) {
                var result = new ArrayList<VeiculoRecord>();
                while (resultSet.next()) {
                    result.add(toVeiculo(resultSet));
                }
                return List.copyOf(result);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private VeiculoRecord buscarVeiculoPostgres(UUID veiculoId) {
        try (var connection = dataSource.getConnection()) {
            return buscarVeiculoPostgres(connection, veiculoId);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private VeiculoRecord atualizarVeiculoPostgres(UUID veiculoId, String placa, String marca, String modelo, int ano) {
        var atual = buscarVeiculoPostgres(veiculoId);
        validarVeiculo(placa, marca, modelo, ano);
        var atualizado = new VeiculoRecord(
                atual.veiculoId(),
                atual.clienteId(),
                placa.trim().toUpperCase(),
                marca.trim(),
                modelo.trim(),
                ano,
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC));
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        UPDATE veiculo
                        SET placa = ?, marca = ?, modelo = ?, ano = ?, atualizado_em = ?
                        WHERE id = ?
                        """)) {
            statement.setString(1, atualizado.placa());
            statement.setString(2, atualizado.marca());
            statement.setString(3, atualizado.modelo());
            statement.setInt(4, atualizado.ano());
            statement.setObject(5, atualizado.atualizadoEm());
            statement.setObject(6, veiculoId);
            statement.executeUpdate();
            return atualizado;
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private OrdemServicoRecord criarOrdemServicoPostgres(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        if (descricaoProblema == null || descricaoProblema.isBlank()) {
            throw new IllegalArgumentException("Descricao do problema e obrigatoria.");
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var ordem = new OrdemServicoRecord(
                UUID.randomUUID(),
                clienteId,
                veiculoId,
                descricaoProblema.trim(),
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                agora,
                agora);
        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                buscarClientePostgres(connection, clienteId);
                var veiculo = buscarVeiculoPostgres(connection, veiculoId);
                if (!veiculo.clienteId().equals(clienteId)) {
                    throw new WebApplicationException("Veiculo nao pertence ao cliente informado.", Response.Status.CONFLICT);
                }
                inserirOrdemServico(connection, ordem);
                inserirHistorico(connection, ordem.ordemServicoId(), ordem.estado(), agora, "Ordem de servico recebida");
                var correlationId = correlationId(null);
                criarSagaInicialPostgres(connection, ordem.ordemServicoId(), agora, correlationId);
                enfileirarEventoPostgres(
                        connection,
                        EVENT_ORDEM_DE_SERVICO_CRIADA,
                        "oficina.os.ordem-de-servico-criada",
                        ordem.ordemServicoId(),
                        Map.of(
                                PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString(),
                                "clienteId", clienteId.toString(),
                                "veiculoId", veiculoId.toString(),
                                PAYLOAD_ESTADO_ATUAL, ordem.estado().name(),
                                "criadoEm", agora.toString(),
                                "descricaoProblema", ordem.descricaoProblema()),
                        correlationId,
                        agora);
                connection.commit();
                return ordem;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private List<OrdemServicoRecord> listarOrdensServicoPostgres(TipoDeEstadoDaOrdemDeServico estado) {
        var sql = """
                SELECT id, cliente_id, veiculo_id, descricao_problema, estado_atual, criado_em, atualizado_em
                FROM ordem_de_servico
                WHERE (? IS NULL OR estado_atual = ?)
                ORDER BY criado_em
                """;
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, estado == null ? null : estado.name());
            statement.setString(2, estado == null ? null : estado.name());
            try (var resultSet = statement.executeQuery()) {
                var result = new ArrayList<OrdemServicoRecord>();
                while (resultSet.next()) {
                    result.add(toOrdemServico(resultSet));
                }
                return List.copyOf(result);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private OrdemServicoRecord buscarOrdemServicoPostgres(UUID ordemServicoId) {
        try (var connection = dataSource.getConnection()) {
            return buscarOrdemServicoPostgres(connection, ordemServicoId);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private List<HistoricoRecord> historicoPostgres(UUID ordemServicoId) {
        buscarOrdemServicoPostgres(ordemServicoId);
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        SELECT tipo_estado, data_estado, motivo
                        FROM estado_ordem_servico
                        WHERE ordem_de_servico_id = ?
                        ORDER BY data_estado
                        """)) {
            statement.setObject(1, ordemServicoId);
            try (var resultSet = statement.executeQuery()) {
                var result = new ArrayList<HistoricoRecord>();
                while (resultSet.next()) {
                    result.add(new HistoricoRecord(
                            TipoDeEstadoDaOrdemDeServico.valueOf(resultSet.getString("tipo_estado")),
                            offsetDateTime(resultSet, "data_estado"),
                            resultSet.getString("motivo")));
                }
                return List.copyOf(result);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private OrdemServicoRecord alterarEstadoPostgres(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                var result = alterarEstadoPostgres(connection, ordemServicoId, novoEstado, motivo, true);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private OperacaoAssincronaRecord cancelarPostgres(UUID ordemServicoId, String motivo) {
        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                buscarOrdemServicoPostgres(connection, ordemServicoId);
                compensarSagaPostgres(connection, ordemServicoId, normalizar(motivo));
                connection.commit();
                return new OperacaoAssincronaRecord("ACEITO", OffsetDateTime.now(ZoneOffset.UTC));
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private SagaRecord buscarSagaPostgres(UUID ordemServicoId) {
        try (var connection = dataSource.getConnection()) {
            buscarOrdemServicoPostgres(connection, ordemServicoId);
            return buscarSagaPostgres(connection, ordemServicoId);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private List<SagaHistoricoRecord> historicoSagaPostgres(UUID ordemServicoId) {
        try (var connection = dataSource.getConnection()) {
            var saga = buscarSagaPostgres(connection, ordemServicoId);
            if (saga == null) {
                return List.of();
            }
            try (var statement = connection.prepareStatement("""
                    SELECT saga_id, estado_anterior, estado_atual, estado_os, etapa, motivo, ocorrido_em
                    FROM saga_estado_historico
                    WHERE saga_id = ?
                    ORDER BY ocorrido_em
                    """)) {
                statement.setObject(1, saga.sagaId());
                try (var resultSet = statement.executeQuery()) {
                    var result = new ArrayList<SagaHistoricoRecord>();
                    while (resultSet.next()) {
                        result.add(toSagaHistorico(resultSet));
                    }
                    return List.copyOf(result);
                }
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private List<OutboxEventRecord> listarOutboxPostgres() {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        SELECT id, aggregate_id, event_type, event_version, topic, producer, payload, status,
                               correlation_id, occurred_at, created_at, published_at, attempts, last_error
                        FROM outbox_event
                        ORDER BY created_at
                        """);
                var resultSet = statement.executeQuery()) {
            var result = new ArrayList<OutboxEventRecord>();
            while (resultSet.next()) {
                result.add(toOutboxEvent(resultSet));
            }
            return List.copyOf(result);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private List<OutboxEventRecord> publicarEventosPendentesPostgres() {
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                var pendentes = new ArrayList<OutboxEventRecord>();
                try (var statement = connection.prepareStatement("""
                        SELECT id, aggregate_id, event_type, event_version, topic, producer, payload, status,
                               correlation_id, occurred_at, created_at, published_at, attempts, last_error
                        FROM outbox_event
                        WHERE status = 'PENDING'
                        ORDER BY created_at
                        FOR UPDATE
                        """);
                        var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        pendentes.add(toOutboxEvent(resultSet));
                    }
                }
                var publicados = new ArrayList<OutboxEventRecord>();
                try (var statement = connection.prepareStatement("""
                        UPDATE outbox_event
                        SET status = 'PUBLISHED', published_at = ?, attempts = ?, next_attempt_at = NULL, last_error = NULL
                        WHERE id = ?
                        """)) {
                    for (var event : pendentes) {
                        var publicado = new OutboxEventRecord(
                                event.eventId(),
                                event.aggregateId(),
                                event.eventType(),
                                event.eventVersion(),
                                event.topic(),
                                event.producer(),
                                event.payload(),
                                "PUBLISHED",
                                event.correlationId(),
                                event.occurredAt(),
                                event.createdAt(),
                                agora,
                                event.attempts() + 1,
                                null);
                        statement.setObject(1, agora);
                        statement.setInt(2, publicado.attempts());
                        statement.setObject(3, publicado.eventId());
                        statement.addBatch();
                        publicados.add(publicado);
                    }
                    statement.executeBatch();
                }
                connection.commit();
                publicados.forEach(event -> logEvent("outbox event published", event, "PUBLISHED"));
                return List.copyOf(publicados);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private SagaRecord consumirEventoPostgres(DomainEventEnvelope event) {
        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                var ordemServicoId = uuidFromPayload(event.payload(), PAYLOAD_ORDEM_SERVICO_ID, event.aggregateId());
                if (inboxContem(connection, event.eventId())) {
                    var saga = buscarSagaPostgres(connection, ordemServicoId);
                    logEvent("domain event ignored", event, "DUPLICATE", ordemServicoId, correlationId(saga, event));
                    connection.commit();
                    return saga;
                }
                var saga = buscarSagaPostgres(connection, ordemServicoId);
                if (saga == null) {
                    throw new NotFoundException("Saga da ordem de servico nao encontrada: " + ordemServicoId);
                }
                inserirInbox(connection, event, ordemServicoId);
                var resultado = switch (event.eventType()) {
                    case "diagnosticoIniciado" -> processarDiagnosticoIniciadoPostgres(connection, saga, event);
                    case "diagnosticoFinalizado" -> processarDiagnosticoFinalizadoPostgres(connection, saga, event);
                    case "orcamentoGerado" -> transicionarSagaPostgres(connection, saga, new SagaTransition(
                            EstadoSaga.AGUARDANDO_APROVACAO,
                            buscarOrdemServicoPostgres(connection, ordemServicoId).estado(),
                            "orcamentoGerado",
                            null,
                            new SagaExternalIds(saga.execucaoId(), uuidFromPayload(event.payload(), PAYLOAD_ORCAMENTO_ID, saga.orcamentoId()), saga.pagamentoId()),
                            event.occurredAt(),
                            correlationId(saga, event)));
                    case "orcamentoAprovado" -> transicionarSagaPostgres(connection, saga, new SagaTransition(
                            EstadoSaga.EM_EXECUCAO,
                            buscarOrdemServicoPostgres(connection, ordemServicoId).estado(),
                            "orcamentoAprovado",
                            null,
                            new SagaExternalIds(saga.execucaoId(), uuidFromPayload(event.payload(), PAYLOAD_ORCAMENTO_ID, saga.orcamentoId()), saga.pagamentoId()),
                            event.occurredAt(),
                            correlationId(saga, event)));
                    case "orcamentoRecusado" -> processarOrcamentoRecusadoPostgres(connection, saga, event);
                    case "execucaoIniciada" -> processarExecucaoIniciadaPostgres(connection, saga, event);
                    case "execucaoFinalizada" -> processarExecucaoFinalizadaPostgres(connection, saga, event);
                    case "pagamentoSolicitado" -> transicionarSagaPostgres(connection, saga, new SagaTransition(
                            EstadoSaga.AGUARDANDO_PAGAMENTO,
                            buscarOrdemServicoPostgres(connection, ordemServicoId).estado(),
                            "pagamentoSolicitado",
                            null,
                            new SagaExternalIds(
                                    saga.execucaoId(),
                                    uuidFromPayload(event.payload(), PAYLOAD_ORCAMENTO_ID, saga.orcamentoId()),
                                    uuidFromPayload(event.payload(), PAYLOAD_PAGAMENTO_ID, saga.pagamentoId())),
                            event.occurredAt(),
                            correlationId(saga, event)));
                    case "pagamentoConfirmado" -> transicionarSagaPostgres(connection, saga, new SagaTransition(
                            EstadoSaga.AGUARDANDO_ENTREGA,
                            buscarOrdemServicoPostgres(connection, ordemServicoId).estado(),
                            "pagamentoConfirmado",
                            null,
                            new SagaExternalIds(
                                    saga.execucaoId(),
                                    saga.orcamentoId(),
                                    uuidFromPayload(event.payload(), PAYLOAD_PAGAMENTO_ID, saga.pagamentoId())),
                            event.occurredAt(),
                            correlationId(saga, event)));
                    case "pagamentoRecusado" -> transicionarSagaPostgres(connection, saga, new SagaTransition(
                            EstadoSaga.AGUARDANDO_PAGAMENTO,
                            buscarOrdemServicoPostgres(connection, ordemServicoId).estado(),
                            "pagamentoRecusado",
                            stringFromPayload(event.payload(), PAYLOAD_MOTIVO),
                            new SagaExternalIds(
                                    saga.execucaoId(),
                                    saga.orcamentoId(),
                                    uuidFromPayload(event.payload(), PAYLOAD_PAGAMENTO_ID, saga.pagamentoId())),
                            event.occurredAt(),
                            correlationId(saga, event)));
                    default -> saga;
                };
                connection.commit();
                logEvent("domain event consumed", event, "CONSUMED", ordemServicoId, correlationId(resultado, event));
                return resultado;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private OrdemServicoRecord alterarEstadoPostgres(
            Connection connection,
            UUID ordemServicoId,
            TipoDeEstadoDaOrdemDeServico novoEstado,
            String motivo,
            boolean finalizarEntrega) throws SQLException {
        var atual = buscarOrdemServicoPostgres(connection, ordemServicoId);
        validarTransicao(atual.estado(), novoEstado);
        var atualizado = new OrdemServicoRecord(
                atual.ordemServicoId(),
                atual.clienteId(),
                atual.veiculoId(),
                atual.descricaoProblema(),
                novoEstado,
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC));
        try (var statement = connection.prepareStatement("""
                UPDATE ordem_de_servico
                SET estado_atual = ?, atualizado_em = ?
                WHERE id = ?
                """)) {
            statement.setString(1, novoEstado.name());
            statement.setObject(2, atualizado.atualizadoEm());
            statement.setObject(3, ordemServicoId);
            statement.executeUpdate();
        }
        inserirHistorico(connection, ordemServicoId, novoEstado, atualizado.atualizadoEm(), normalizar(motivo));
        if (finalizarEntrega && novoEstado == TipoDeEstadoDaOrdemDeServico.ENTREGUE) {
            finalizarSagaComEntregaPostgres(connection, atualizado, normalizar(motivo));
        }
        return atualizado;
    }

    private void compensarSagaPostgres(Connection connection, UUID ordemServicoId, String motivo) throws SQLException {
        var saga = buscarSagaPostgres(connection, ordemServicoId);
        if (saga == null || saga.estado() == EstadoSaga.COMPENSADA || saga.estado() == EstadoSaga.FINALIZADA_COM_SUCESSO) {
            return;
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var emCompensacao = transicionarSagaPostgres(connection, saga, new SagaTransition(
                EstadoSaga.EM_COMPENSACAO,
                buscarOrdemServicoPostgres(connection, ordemServicoId).estado(),
                "cancelamentoSolicitado",
                motivo,
                new SagaExternalIds(saga.execucaoId(), saga.orcamentoId(), saga.pagamentoId()),
                agora,
                saga.correlationId()));
        transicionarSagaPostgres(connection, emCompensacao, new SagaTransition(
                EstadoSaga.COMPENSADA,
                buscarOrdemServicoPostgres(connection, ordemServicoId).estado(),
                EVENT_SAGA_COMPENSADA,
                motivo,
                new SagaExternalIds(saga.execucaoId(), saga.orcamentoId(), saga.pagamentoId()),
                agora,
                saga.correlationId()));
        enfileirarEventoPostgres(
                connection,
                EVENT_SAGA_COMPENSADA,
                "oficina.saga.saga-compensada",
                ordemServicoId,
                Map.of(
                        "sagaId", saga.sagaId().toString(),
                        PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId.toString(),
                        PAYLOAD_MOTIVO, motivo == null ? "Cancelamento solicitado" : motivo,
                        "compensadaEm", agora.toString()),
                saga.correlationId(),
                agora);
    }

    private SagaRecord processarDiagnosticoIniciadoPostgres(Connection connection, SagaRecord saga, DomainEventEnvelope event) throws SQLException {
        var ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.RECEBIDA) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, "Diagnostico iniciado", false);
        }
        return transicionarSagaPostgres(connection, saga, new SagaTransition(
                EstadoSaga.EM_DIAGNOSTICO,
                TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO,
                "diagnosticoIniciado",
                null,
                new SagaExternalIds(
                        uuidFromPayload(event.payload(), PAYLOAD_EXECUCAO_ID, saga.execucaoId()),
                        saga.orcamentoId(),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private SagaRecord processarDiagnosticoFinalizadoPostgres(Connection connection, SagaRecord saga, DomainEventEnvelope event) throws SQLException {
        var ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO, "Diagnostico finalizado", false);
        }
        return transicionarSagaPostgres(connection, saga, new SagaTransition(
                EstadoSaga.AGUARDANDO_ORCAMENTO,
                TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO,
                "diagnosticoFinalizado",
                null,
                new SagaExternalIds(
                        uuidFromPayload(event.payload(), PAYLOAD_EXECUCAO_ID, saga.execucaoId()),
                        saga.orcamentoId(),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private SagaRecord processarOrcamentoRecusadoPostgres(Connection connection, SagaRecord saga, DomainEventEnvelope event) throws SQLException {
        var ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, "Orcamento recusado", false);
        }
        return transicionarSagaPostgres(connection, saga, new SagaTransition(
                EstadoSaga.EM_DIAGNOSTICO,
                TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO,
                "orcamentoRecusado",
                stringFromPayload(event.payload(), PAYLOAD_MOTIVO),
                new SagaExternalIds(
                        saga.execucaoId(),
                        uuidFromPayload(event.payload(), PAYLOAD_ORCAMENTO_ID, saga.orcamentoId()),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private SagaRecord processarExecucaoIniciadaPostgres(Connection connection, SagaRecord saga, DomainEventEnvelope event) throws SQLException {
        var ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, "Execucao iniciada", false);
        }
        return transicionarSagaPostgres(connection, saga, new SagaTransition(
                EstadoSaga.EM_EXECUCAO,
                TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO,
                "execucaoIniciada",
                null,
                new SagaExternalIds(
                        uuidFromPayload(event.payload(), PAYLOAD_EXECUCAO_ID, saga.execucaoId()),
                        saga.orcamentoId(),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private SagaRecord processarExecucaoFinalizadaPostgres(Connection connection, SagaRecord saga, DomainEventEnvelope event) throws SQLException {
        var ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO) {
            ordem = alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.FINALIZADA, "Execucao finalizada", false);
        }
        enfileirarEventoPostgres(
                connection,
                "ordemDeServicoFinalizada",
                "oficina.os.ordem-de-servico-finalizada",
                ordem.ordemServicoId(),
                Map.of(
                        PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString(),
                        "estadoAnterior", TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO.name(),
                        PAYLOAD_ESTADO_ATUAL, TipoDeEstadoDaOrdemDeServico.FINALIZADA.name(),
                        "finalizadaEm", OffsetDateTime.now(ZoneOffset.UTC).toString()),
                correlationId(saga, event),
                OffsetDateTime.now(ZoneOffset.UTC));
        return transicionarSagaPostgres(connection, saga, new SagaTransition(
                EstadoSaga.AGUARDANDO_PAGAMENTO,
                TipoDeEstadoDaOrdemDeServico.FINALIZADA,
                "execucaoFinalizada",
                null,
                new SagaExternalIds(
                        uuidFromPayload(event.payload(), PAYLOAD_EXECUCAO_ID, saga.execucaoId()),
                        saga.orcamentoId(),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private void finalizarSagaComEntregaPostgres(Connection connection, OrdemServicoRecord ordem, String motivo) throws SQLException {
        var saga = buscarSagaPostgres(connection, ordem.ordemServicoId());
        if (saga == null || saga.estado() == EstadoSaga.FINALIZADA_COM_SUCESSO) {
            return;
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        enfileirarEventoPostgres(
                connection,
                "ordemDeServicoEntregue",
                "oficina.os.ordem-de-servico-entregue",
                ordem.ordemServicoId(),
                Map.of(
                        PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString(),
                        "estadoAnterior", TipoDeEstadoDaOrdemDeServico.FINALIZADA.name(),
                        PAYLOAD_ESTADO_ATUAL, TipoDeEstadoDaOrdemDeServico.ENTREGUE.name(),
                        "entregueEm", agora.toString()),
                saga.correlationId(),
                agora);
        enfileirarEventoPostgres(
                connection,
                "sagaFinalizadaComSucesso",
                "oficina.saga.saga-finalizada-com-sucesso",
                ordem.ordemServicoId(),
                Map.of(
                        "sagaId", saga.sagaId().toString(),
                        PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString(),
                        "finalizadaEm", agora.toString()),
                saga.correlationId(),
                agora);
        transicionarSagaPostgres(connection, saga, new SagaTransition(
                EstadoSaga.FINALIZADA_COM_SUCESSO,
                TipoDeEstadoDaOrdemDeServico.ENTREGUE,
                "entregaFinalizada",
                motivo,
                new SagaExternalIds(saga.execucaoId(), saga.orcamentoId(), saga.pagamentoId()),
                agora,
                saga.correlationId()));
    }

    private SagaRecord criarSagaInicialPostgres(Connection connection, UUID ordemServicoId, OffsetDateTime agora, String correlationId) throws SQLException {
        var saga = new SagaRecord(
                UUID.randomUUID(),
                ordemServicoId,
                EstadoSaga.INICIADA,
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                EVENT_ORDEM_DE_SERVICO_CRIADA,
                null,
                null,
                null,
                correlationId,
                agora,
                agora,
                null);
        try (var statement = connection.prepareStatement("""
                INSERT INTO saga_ordem_servico (
                    id, ordem_de_servico_id, estado, estado_os, ultima_etapa,
                    execucao_id, orcamento_id, pagamento_id, correlation_id,
                    criado_em, atualizado_em, ultimo_erro
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            bindSaga(statement, saga);
            statement.executeUpdate();
        }
        inserirSagaHistorico(connection, new SagaHistoricoRecord(
                saga.sagaId(),
                null,
                EstadoSaga.INICIADA,
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                EVENT_ORDEM_DE_SERVICO_CRIADA,
                null,
                agora));
        return saga;
    }

    private SagaRecord transicionarSagaPostgres(Connection connection, SagaRecord saga, SagaTransition transition) throws SQLException {
        if (saga.estado() == transition.novoEstado() && transition.etapa().equals(saga.ultimaEtapa())) {
            return saga;
        }
        var atualizada = new SagaRecord(
                saga.sagaId(),
                saga.ordemServicoId(),
                transition.novoEstado(),
                transition.estadoOrdemServico(),
                transition.etapa(),
                transition.ids().execucaoId(),
                transition.ids().orcamentoId(),
                transition.ids().pagamentoId(),
                transition.correlationId(),
                saga.criadoEm(),
                transition.ocorridoEm(),
                transition.motivo());
        try (var statement = connection.prepareStatement("""
                UPDATE saga_ordem_servico
                SET estado = ?, estado_os = ?, ultima_etapa = ?, execucao_id = ?, orcamento_id = ?,
                    pagamento_id = ?, correlation_id = ?, atualizado_em = ?, ultimo_erro = ?
                WHERE id = ?
                """)) {
            statement.setString(1, atualizada.estado().name());
            statement.setString(2, atualizada.estadoOrdemServico().name());
            statement.setString(3, atualizada.ultimaEtapa());
            setUuid(statement, 4, atualizada.execucaoId());
            setUuid(statement, 5, atualizada.orcamentoId());
            setUuid(statement, 6, atualizada.pagamentoId());
            statement.setString(7, atualizada.correlationId());
            statement.setObject(8, atualizada.atualizadoEm());
            statement.setString(9, atualizada.ultimoErro());
            statement.setObject(10, atualizada.sagaId());
            statement.executeUpdate();
        }
        inserirSagaHistorico(connection, new SagaHistoricoRecord(
                saga.sagaId(),
                saga.estado(),
                transition.novoEstado(),
                transition.estadoOrdemServico(),
                transition.etapa(),
                transition.motivo(),
                transition.ocorridoEm()));
        return atualizada;
    }

    private void enfileirarEventoPostgres(
            Connection connection,
            String eventType,
            String topic,
            UUID aggregateId,
            Map<String, Object> payload,
            String correlationId,
            OffsetDateTime occurredAt) throws SQLException {
        var effectiveCorrelationId = correlationId(correlationId);
        var event = new OutboxEventRecord(
                UUID.randomUUID(),
                aggregateId,
                eventType,
                1,
                topic,
                PRODUCER,
                payload,
                "PENDING",
                effectiveCorrelationId,
                occurredAt,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0,
                null);
        try (var statement = connection.prepareStatement("""
                INSERT INTO outbox_event (
                    id, aggregate_id, event_type, event_version, topic, producer, payload, status,
                    correlation_id, occurred_at, created_at, published_at, attempts, next_attempt_at, last_error
                ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, NULL, ?)
                """)) {
            statement.setObject(1, event.eventId());
            statement.setString(2, event.aggregateId().toString());
            statement.setString(3, event.eventType());
            statement.setInt(4, event.eventVersion());
            statement.setString(5, event.topic());
            statement.setString(6, event.producer());
            statement.setString(7, toJson(event.payload()));
            statement.setString(8, event.status());
            statement.setString(9, event.correlationId());
            statement.setObject(10, event.occurredAt());
            statement.setObject(11, event.createdAt());
            statement.setObject(12, event.publishedAt());
            statement.setInt(13, event.attempts());
            statement.setString(14, event.lastError());
            statement.executeUpdate();
        }
        logEvent("outbox event registered", event, "PENDING");
    }

    private void inserirOrdemServico(Connection connection, OrdemServicoRecord ordem) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO ordem_de_servico (id, cliente_id, veiculo_id, descricao_problema, estado_atual, criado_em, atualizado_em)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, ordem.ordemServicoId());
            statement.setObject(2, ordem.clienteId());
            statement.setObject(3, ordem.veiculoId());
            statement.setString(4, ordem.descricaoProblema());
            statement.setString(5, ordem.estado().name());
            statement.setObject(6, ordem.criadoEm());
            statement.setObject(7, ordem.atualizadoEm());
            statement.executeUpdate();
        }
    }

    private void inserirHistorico(
            Connection connection,
            UUID ordemServicoId,
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime dataEstado,
            String motivo) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO estado_ordem_servico (id, ordem_de_servico_id, tipo_estado, data_estado, motivo)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, ordemServicoId);
            statement.setString(3, estado.name());
            statement.setObject(4, dataEstado);
            statement.setString(5, motivo);
            statement.executeUpdate();
        }
    }

    private void inserirSagaHistorico(Connection connection, SagaHistoricoRecord historico) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO saga_estado_historico (id, saga_id, estado_anterior, estado_atual, estado_os, etapa, motivo, ocorrido_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, historico.sagaId());
            statement.setString(3, historico.estadoAnterior() == null ? null : historico.estadoAnterior().name());
            statement.setString(4, historico.estadoAtual().name());
            statement.setString(5, historico.estadoOrdemServico().name());
            statement.setString(6, historico.etapa());
            statement.setString(7, historico.motivo());
            statement.setObject(8, historico.ocorridoEm());
            statement.executeUpdate();
        }
    }

    private ClienteRecord buscarClientePostgres(Connection connection, UUID clienteId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT c.id AS cliente_id, p.nome, p.documento, c.telefone, c.email, c.criado_em, c.atualizado_em
                FROM cliente c
                JOIN pessoa p ON p.id = c.pessoa_id
                WHERE c.id = ?
                """)) {
            statement.setObject(1, clienteId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Cliente nao encontrado: " + clienteId);
                }
                return toCliente(resultSet);
            }
        }
    }

    private UUID pessoaIdDoCliente(Connection connection, UUID clienteId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT pessoa_id FROM cliente WHERE id = ?")) {
            statement.setObject(1, clienteId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Cliente nao encontrado: " + clienteId);
                }
                return uuid(resultSet, "pessoa_id");
            }
        }
    }

    private VeiculoRecord buscarVeiculoPostgres(Connection connection, UUID veiculoId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT id, cliente_id, placa, marca, modelo, ano, criado_em, atualizado_em
                FROM veiculo
                WHERE id = ?
                """)) {
            statement.setObject(1, veiculoId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Veiculo nao encontrado: " + veiculoId);
                }
                return toVeiculo(resultSet);
            }
        }
    }

    private OrdemServicoRecord buscarOrdemServicoPostgres(Connection connection, UUID ordemServicoId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT id, cliente_id, veiculo_id, descricao_problema, estado_atual, criado_em, atualizado_em
                FROM ordem_de_servico
                WHERE id = ?
                """)) {
            statement.setObject(1, ordemServicoId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("Ordem de servico nao encontrada: " + ordemServicoId);
                }
                return toOrdemServico(resultSet);
            }
        }
    }

    private SagaRecord buscarSagaPostgres(Connection connection, UUID ordemServicoId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT id, ordem_de_servico_id, estado, estado_os, ultima_etapa, execucao_id, orcamento_id,
                       pagamento_id, correlation_id, criado_em, atualizado_em, ultimo_erro
                FROM saga_ordem_servico
                WHERE ordem_de_servico_id = ?
                """)) {
            statement.setObject(1, ordemServicoId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? toSaga(resultSet) : null;
            }
        }
    }

    private boolean inboxContem(Connection connection, UUID eventId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT 1 FROM inbox_event WHERE event_id = ?")) {
            statement.setObject(1, eventId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void inserirInbox(Connection connection, DomainEventEnvelope event, UUID ordemServicoId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO inbox_event (
                    event_id, event_type, event_version, producer, aggregate_id, correlation_id,
                    payload, occurred_at, consumed_at, status, last_error
                ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, 'CONSUMED', NULL)
                """)) {
            statement.setObject(1, event.eventId());
            statement.setString(2, event.eventType());
            statement.setInt(3, event.eventVersion());
            statement.setString(4, event.producer());
            statement.setObject(5, ordemServicoId);
            statement.setString(6, correlationId((String) null));
            statement.setString(7, toJson(event.payload()));
            statement.setObject(8, event.occurredAt());
            statement.setObject(9, OffsetDateTime.now(ZoneOffset.UTC));
            statement.executeUpdate();
        }
    }

    private void bindSaga(java.sql.PreparedStatement statement, SagaRecord saga) throws SQLException {
        statement.setObject(1, saga.sagaId());
        statement.setObject(2, saga.ordemServicoId());
        statement.setString(3, saga.estado().name());
        statement.setString(4, saga.estadoOrdemServico().name());
        statement.setString(5, saga.ultimaEtapa());
        setUuid(statement, 6, saga.execucaoId());
        setUuid(statement, 7, saga.orcamentoId());
        setUuid(statement, 8, saga.pagamentoId());
        statement.setString(9, saga.correlationId());
        statement.setObject(10, saga.criadoEm());
        statement.setObject(11, saga.atualizadoEm());
        statement.setString(12, saga.ultimoErro());
    }

    private ClienteRecord toCliente(ResultSet resultSet) throws SQLException {
        return new ClienteRecord(
                uuid(resultSet, "cliente_id"),
                resultSet.getString("nome"),
                resultSet.getString("documento"),
                resultSet.getString("telefone"),
                resultSet.getString("email"),
                offsetDateTime(resultSet, "criado_em"),
                offsetDateTime(resultSet, "atualizado_em"));
    }

    private VeiculoRecord toVeiculo(ResultSet resultSet) throws SQLException {
        return new VeiculoRecord(
                uuid(resultSet, "id"),
                uuid(resultSet, "cliente_id"),
                resultSet.getString("placa"),
                resultSet.getString("marca"),
                resultSet.getString("modelo"),
                resultSet.getInt("ano"),
                offsetDateTime(resultSet, "criado_em"),
                offsetDateTime(resultSet, "atualizado_em"));
    }

    private OrdemServicoRecord toOrdemServico(ResultSet resultSet) throws SQLException {
        return new OrdemServicoRecord(
                uuid(resultSet, "id"),
                uuid(resultSet, "cliente_id"),
                uuid(resultSet, "veiculo_id"),
                resultSet.getString("descricao_problema"),
                TipoDeEstadoDaOrdemDeServico.valueOf(resultSet.getString("estado_atual")),
                offsetDateTime(resultSet, "criado_em"),
                offsetDateTime(resultSet, "atualizado_em"));
    }

    private SagaRecord toSaga(ResultSet resultSet) throws SQLException {
        return new SagaRecord(
                uuid(resultSet, "id"),
                uuid(resultSet, "ordem_de_servico_id"),
                EstadoSaga.valueOf(resultSet.getString("estado")),
                TipoDeEstadoDaOrdemDeServico.valueOf(resultSet.getString("estado_os")),
                resultSet.getString("ultima_etapa"),
                uuid(resultSet, "execucao_id"),
                uuid(resultSet, "orcamento_id"),
                uuid(resultSet, "pagamento_id"),
                resultSet.getString("correlation_id"),
                offsetDateTime(resultSet, "criado_em"),
                offsetDateTime(resultSet, "atualizado_em"),
                resultSet.getString("ultimo_erro"));
    }

    private SagaHistoricoRecord toSagaHistorico(ResultSet resultSet) throws SQLException {
        var estadoAnterior = resultSet.getString("estado_anterior");
        return new SagaHistoricoRecord(
                uuid(resultSet, "saga_id"),
                estadoAnterior == null ? null : EstadoSaga.valueOf(estadoAnterior),
                EstadoSaga.valueOf(resultSet.getString("estado_atual")),
                TipoDeEstadoDaOrdemDeServico.valueOf(resultSet.getString("estado_os")),
                resultSet.getString("etapa"),
                resultSet.getString("motivo"),
                offsetDateTime(resultSet, "ocorrido_em"));
    }

    private OutboxEventRecord toOutboxEvent(ResultSet resultSet) throws SQLException {
        return new OutboxEventRecord(
                uuid(resultSet, "id"),
                UUID.fromString(resultSet.getString("aggregate_id")),
                resultSet.getString("event_type"),
                resultSet.getInt("event_version"),
                resultSet.getString("topic"),
                resultSet.getString("producer"),
                fromJson(resultSet.getString("payload")),
                resultSet.getString("status"),
                resultSet.getString("correlation_id"),
                offsetDateTime(resultSet, "occurred_at"),
                offsetDateTime(resultSet, "created_at"),
                nullableOffsetDateTime(resultSet, "published_at"),
                resultSet.getInt("attempts"),
                resultSet.getString("last_error"));
    }

    private static UUID uuid(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, UUID.class);
    }

    private static OffsetDateTime offsetDateTime(ResultSet resultSet, String column) throws SQLException {
        var value = resultSet.getObject(column);
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new SQLException("Coluna " + column + " nao pode ser convertida para OffsetDateTime.");
    }

    private static OffsetDateTime nullableOffsetDateTime(ResultSet resultSet, String column) throws SQLException {
        var value = resultSet.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new SQLException("Coluna " + column + " nao pode ser convertida para OffsetDateTime.");
    }

    private static void setUuid(java.sql.PreparedStatement statement, int index, UUID value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.OTHER);
        } else {
            statement.setObject(index, value);
        }
    }

    private static String toJson(Map<String, Object> payload) throws SQLException {
        try {
            return JSON.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new SQLException("Payload de evento invalido.", exception);
        }
    }

    private static Map<String, Object> fromJson(String payload) throws SQLException {
        try {
            return JSON.readValue(payload, JSON_MAP);
        } catch (JsonProcessingException exception) {
            throw new SQLException("Payload de evento invalido.", exception);
        }
    }

    private static IllegalStateException persistenceFailure(SQLException exception) {
        return new IllegalStateException("Falha ao acessar PostgreSQL do oficina-os-service.", exception);
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // The original persistence failure is more useful to callers.
        }
    }

    private void compensarSaga(UUID ordemServicoId, String motivo) {
        var saga = sagasByOrdemServico.get(ordemServicoId);
        if (saga == null || saga.estado() == EstadoSaga.COMPENSADA || saga.estado() == EstadoSaga.FINALIZADA_COM_SUCESSO) {
            return;
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var emCompensacao = transicionarSaga(saga, new SagaTransition(
                EstadoSaga.EM_COMPENSACAO,
                buscarOrdemServico(ordemServicoId).estado(),
                "cancelamentoSolicitado",
                motivo,
                new SagaExternalIds(saga.execucaoId(), saga.orcamentoId(), saga.pagamentoId()),
                agora,
                saga.correlationId()));
        transicionarSaga(emCompensacao, new SagaTransition(
                EstadoSaga.COMPENSADA,
                buscarOrdemServico(ordemServicoId).estado(),
                EVENT_SAGA_COMPENSADA,
                motivo,
                new SagaExternalIds(saga.execucaoId(), saga.orcamentoId(), saga.pagamentoId()),
                agora,
                saga.correlationId()));
        enfileirarEvento(
                EVENT_SAGA_COMPENSADA,
                "oficina.saga.saga-compensada",
                ordemServicoId,
                Map.of(
                        "sagaId", saga.sagaId().toString(),
                        PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId.toString(),
                        PAYLOAD_MOTIVO, motivo == null ? "Cancelamento solicitado" : motivo,
                        "compensadaEm", agora.toString()),
                saga.correlationId(),
                agora);
    }

    private SagaRecord processarDiagnosticoIniciado(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.RECEBIDA) {
            alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, "Diagnostico iniciado");
        }
        return transicionarSaga(saga, new SagaTransition(
                EstadoSaga.EM_DIAGNOSTICO,
                TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO,
                "diagnosticoIniciado",
                null,
                new SagaExternalIds(
                        uuidFromPayload(event.payload(), PAYLOAD_EXECUCAO_ID, saga.execucaoId()),
                        saga.orcamentoId(),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private SagaRecord processarDiagnosticoFinalizado(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO) {
            alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO, "Diagnostico finalizado");
        }
        return transicionarSaga(saga, new SagaTransition(
                EstadoSaga.AGUARDANDO_ORCAMENTO,
                TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO,
                "diagnosticoFinalizado",
                null,
                new SagaExternalIds(
                        uuidFromPayload(event.payload(), PAYLOAD_EXECUCAO_ID, saga.execucaoId()),
                        saga.orcamentoId(),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private SagaRecord processarOrcamentoRecusado(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO) {
            alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, "Orcamento recusado");
        }
        return transicionarSaga(saga, new SagaTransition(
                EstadoSaga.EM_DIAGNOSTICO,
                TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO,
                "orcamentoRecusado",
                stringFromPayload(event.payload(), PAYLOAD_MOTIVO),
                new SagaExternalIds(
                        saga.execucaoId(),
                        uuidFromPayload(event.payload(), PAYLOAD_ORCAMENTO_ID, saga.orcamentoId()),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private SagaRecord processarExecucaoIniciada(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO) {
            alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, "Execucao iniciada");
        }
        return transicionarSaga(saga, new SagaTransition(
                EstadoSaga.EM_EXECUCAO,
                TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO,
                "execucaoIniciada",
                null,
                new SagaExternalIds(
                        uuidFromPayload(event.payload(), PAYLOAD_EXECUCAO_ID, saga.execucaoId()),
                        saga.orcamentoId(),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private SagaRecord processarExecucaoFinalizada(SagaRecord saga, DomainEventEnvelope event) {
        var ordem = buscarOrdemServico(saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO) {
            ordem = alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.FINALIZADA, "Execucao finalizada");
        }
        enfileirarEvento(
                "ordemDeServicoFinalizada",
                "oficina.os.ordem-de-servico-finalizada",
                ordem.ordemServicoId(),
                Map.of(
                        PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString(),
                        "estadoAnterior", TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO.name(),
                        PAYLOAD_ESTADO_ATUAL, TipoDeEstadoDaOrdemDeServico.FINALIZADA.name(),
                        "finalizadaEm", OffsetDateTime.now(ZoneOffset.UTC).toString()),
                correlationId(saga, event),
                OffsetDateTime.now(ZoneOffset.UTC));
        return transicionarSaga(saga, new SagaTransition(
                EstadoSaga.AGUARDANDO_PAGAMENTO,
                TipoDeEstadoDaOrdemDeServico.FINALIZADA,
                "execucaoFinalizada",
                null,
                new SagaExternalIds(
                        uuidFromPayload(event.payload(), PAYLOAD_EXECUCAO_ID, saga.execucaoId()),
                        saga.orcamentoId(),
                        saga.pagamentoId()),
                event.occurredAt(),
                correlationId(saga, event)));
    }

    private void finalizarSagaComEntrega(OrdemServicoRecord ordem, String motivo) {
        var saga = sagasByOrdemServico.get(ordem.ordemServicoId());
        if (saga == null || saga.estado() == EstadoSaga.FINALIZADA_COM_SUCESSO) {
            return;
        }
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        enfileirarEvento(
                "ordemDeServicoEntregue",
                "oficina.os.ordem-de-servico-entregue",
                ordem.ordemServicoId(),
                Map.of(
                        PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString(),
                        "estadoAnterior", TipoDeEstadoDaOrdemDeServico.FINALIZADA.name(),
                        PAYLOAD_ESTADO_ATUAL, TipoDeEstadoDaOrdemDeServico.ENTREGUE.name(),
                        "entregueEm", agora.toString()),
                saga.correlationId(),
                agora);
        enfileirarEvento(
                "sagaFinalizadaComSucesso",
                "oficina.saga.saga-finalizada-com-sucesso",
                ordem.ordemServicoId(),
                Map.of(
                        "sagaId", saga.sagaId().toString(),
                        PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString(),
                        "finalizadaEm", agora.toString()),
                saga.correlationId(),
                agora);
        transicionarSaga(saga, new SagaTransition(
                EstadoSaga.FINALIZADA_COM_SUCESSO,
                TipoDeEstadoDaOrdemDeServico.ENTREGUE,
                "entregaFinalizada",
                motivo,
                new SagaExternalIds(saga.execucaoId(), saga.orcamentoId(), saga.pagamentoId()),
                agora,
                saga.correlationId()));
    }

    private SagaRecord criarSagaInicial(UUID ordemServicoId, OffsetDateTime agora, String correlationId) {
        var saga = new SagaRecord(
                UUID.randomUUID(),
                ordemServicoId,
                EstadoSaga.INICIADA,
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                EVENT_ORDEM_DE_SERVICO_CRIADA,
                null,
                null,
                null,
                correlationId,
                agora,
                agora,
                null);
        sagasByOrdemServico.put(ordemServicoId, saga);
        sagaHistoricos.put(saga.sagaId(), new ArrayList<>(List.of(new SagaHistoricoRecord(
                saga.sagaId(),
                null,
                EstadoSaga.INICIADA,
                TipoDeEstadoDaOrdemDeServico.RECEBIDA,
                EVENT_ORDEM_DE_SERVICO_CRIADA,
                null,
                agora))));
        return saga;
    }

    private SagaRecord transicionarSaga(SagaRecord saga, SagaTransition transition) {
        if (saga.estado() == transition.novoEstado() && transition.etapa().equals(saga.ultimaEtapa())) {
            return saga;
        }
        var atualizada = new SagaRecord(
                saga.sagaId(),
                saga.ordemServicoId(),
                transition.novoEstado(),
                transition.estadoOrdemServico(),
                transition.etapa(),
                transition.ids().execucaoId(),
                transition.ids().orcamentoId(),
                transition.ids().pagamentoId(),
                transition.correlationId(),
                saga.criadoEm(),
                transition.ocorridoEm(),
                transition.motivo());
        sagasByOrdemServico.put(saga.ordemServicoId(), atualizada);
        sagaHistoricos.computeIfAbsent(saga.sagaId(), _ -> new ArrayList<>())
                .add(new SagaHistoricoRecord(
                        saga.sagaId(),
                        saga.estado(),
                        transition.novoEstado(),
                        transition.estadoOrdemServico(),
                        transition.etapa(),
                        transition.motivo(),
                        transition.ocorridoEm()));
        return atualizada;
    }

    private void enfileirarEvento(
            String eventType,
            String topic,
            UUID aggregateId,
            Map<String, Object> payload,
            String correlationId,
            OffsetDateTime occurredAt) {
        var effectiveCorrelationId = correlationId(correlationId);
        var event = new OutboxEventRecord(
                UUID.randomUUID(),
                aggregateId,
                eventType,
                1,
                topic,
                PRODUCER,
                payload,
                "PENDING",
                effectiveCorrelationId,
                occurredAt,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0,
                null);
        outboxEvents.put(event.eventId(), event);
        logEvent("outbox event registered", event, "PENDING");
    }

    private void logEvent(String message, OutboxEventRecord event, String messageStatus) {
        StructuredLog.info(LOG, message, Map.of(
                "correlationId", event.correlationId(),
                "eventId", event.eventId().toString(),
                "eventType", event.eventType(),
                "eventVersion", event.eventVersion(),
                "topic", event.topic(),
                "producer", event.producer(),
                "aggregateId", event.aggregateId().toString(),
                "messageStatus", messageStatus));
    }

    private void logEvent(
            String message,
            DomainEventEnvelope event,
            String messageStatus,
            UUID aggregateId,
            String correlationId) {
        StructuredLog.info(LOG, message, Map.of(
                "correlationId", correlationId(correlationId),
                "eventId", event.eventId().toString(),
                "eventType", event.eventType(),
                "eventVersion", event.eventVersion(),
                "producer", event.producer(),
                "consumer", PRODUCER,
                "aggregateId", aggregateId.toString(),
                "messageStatus", messageStatus));
    }

    private String correlationId(SagaRecord saga, DomainEventEnvelope event) {
        if (saga != null && saga.correlationId() != null && !saga.correlationId().isBlank()) {
            return saga.correlationId();
        }
        return event.eventId().toString();
    }

    private String correlationId(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId.trim();
        }
        var mdcCorrelationId = MDC.get("correlationId");
        if (mdcCorrelationId != null && !mdcCorrelationId.toString().isBlank()) {
            return mdcCorrelationId.toString();
        }
        return "local-" + UUID.randomUUID();
    }

    private static UUID uuidFromPayload(Map<String, Object> payload, String fieldName, UUID fallback) {
        var value = payload.get(fieldName);
        if (value == null) {
            return fallback;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static String stringFromPayload(Map<String, Object> payload, String fieldName) {
        var value = payload.get(fieldName);
        return value == null ? null : value.toString();
    }

    private static void validarCliente(String nome, String documento, String email) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente e obrigatorio.");
        }
        DocumentoFactory.from(documento);
        if (email != null && !email.isBlank()) {
            new Email(email);
        }
    }

    private static void validarVeiculo(String placa, String marca, String modelo, int ano) {
        new PlacaDeVeiculo(placa);
        new MarcaDeVeiculo(marca);
        new ModeloDeVeiculo(modelo);
        if (ano < 1900) {
            throw new IllegalArgumentException("Ano do veiculo deve ser maior ou igual a 1900.");
        }
    }

    private static void validarTransicao(TipoDeEstadoDaOrdemDeServico atual, TipoDeEstadoDaOrdemDeServico novo) {
        boolean valida = switch (atual) {
            case RECEBIDA -> novo == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO -> novo == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> novo == TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO
                    || novo == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO;
            case EM_EXECUCAO -> novo == TipoDeEstadoDaOrdemDeServico.FINALIZADA;
            case FINALIZADA -> novo == TipoDeEstadoDaOrdemDeServico.ENTREGUE;
            case ENTREGUE -> false;
        };
        if (!valida) {
            throw new WebApplicationException("Transicao de estado invalida: " + atual + " -> " + novo, Response.Status.CONFLICT);
        }
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private record SagaExternalIds(
            UUID execucaoId,
            UUID orcamentoId,
            UUID pagamentoId) {
    }

    private record SagaTransition(
            EstadoSaga novoEstado,
            TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
            String etapa,
            String motivo,
            SagaExternalIds ids,
            OffsetDateTime ocorridoEm,
            String correlationId) {
    }

    public record ClienteRecord(
            UUID clienteId,
            String nome,
            String documento,
            String telefone,
            String email,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record VeiculoRecord(
            UUID veiculoId,
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            int ano,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record OrdemServicoRecord(
            UUID ordemServicoId,
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema,
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record HistoricoRecord(
            TipoDeEstadoDaOrdemDeServico estado,
            OffsetDateTime dataDoEstado,
            String motivo) {
    }

    public record OperacaoAssincronaRecord(
            String status,
            OffsetDateTime solicitadoEm) {
    }

    public record SagaRecord(
            UUID sagaId,
            UUID ordemServicoId,
            EstadoSaga estado,
            TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
            String ultimaEtapa,
            UUID execucaoId,
            UUID orcamentoId,
            UUID pagamentoId,
            String correlationId,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm,
            String ultimoErro) {
    }

    public record SagaHistoricoRecord(
            UUID sagaId,
            EstadoSaga estadoAnterior,
            EstadoSaga estadoAtual,
            TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
            String etapa,
            String motivo,
            OffsetDateTime ocorridoEm) {
    }
}

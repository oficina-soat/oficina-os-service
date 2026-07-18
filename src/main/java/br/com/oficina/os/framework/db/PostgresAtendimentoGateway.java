package br.com.oficina.os.framework.db;

import static br.com.oficina.os.framework.db.AtendimentoEventLog.correlationId;
import static br.com.oficina.os.framework.db.AtendimentoEventLog.logEvent;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.*;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.jboss.logging.Logger;

class PostgresAtendimentoGateway implements AtendimentoGateway {
    private static final Logger LOG = Logger.getLogger(PostgresAtendimentoGateway.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, Object>> JSON_MAP = new TypeReference<>() {
    };
    private static final String COLUMN_CLIENTE_ID = "cliente_id";
    private static final String COLUMN_CRIADO_EM = "criado_em";
    private static final String COLUMN_ATUALIZADO_EM = "atualizado_em";
    private static final String SQL_SELECT_OUTBOX = """
            SELECT id, aggregate_id, event_type, event_version, topic, producer, payload, status,
                   correlation_id, occurred_at, created_at, published_at, attempts, last_error
            FROM outbox_event
            ORDER BY created_at
            """;
    private static final String SQL_SELECT_PENDING_OUTBOX_FOR_PUBLICATION = """
            SELECT id, aggregate_id, event_type, event_version, topic, producer, payload, status,
                   correlation_id, occurred_at, created_at, published_at, attempts, last_error
            FROM outbox_event
            WHERE status = ?
              AND (next_attempt_at IS NULL OR next_attempt_at <= ?)
            ORDER BY created_at
            LIMIT ?
            """;
    private static final String SQL_CLAIM_PENDING_OUTBOX = """
            WITH candidates AS (
                SELECT id FROM outbox_event
                WHERE status = ?
                  AND (next_attempt_at IS NULL OR next_attempt_at <= ?)
                  AND (claim_until IS NULL OR claim_until <= ?)
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            )
            UPDATE outbox_event AS outbox
            SET claim_owner = ?, claim_until = ?
            FROM candidates
            WHERE outbox.id = candidates.id
            RETURNING outbox.id, outbox.aggregate_id, outbox.event_type, outbox.event_version,
                      outbox.topic, outbox.producer, outbox.payload, outbox.status,
                      outbox.correlation_id, outbox.occurred_at, outbox.created_at,
                      outbox.published_at, outbox.attempts, outbox.last_error
            """;
    private static final String SQL_SELECT_OUTBOX_BY_ID_FOR_UPDATE = """
            SELECT id, aggregate_id, event_type, event_version, topic, producer, payload, status,
                   correlation_id, occurred_at, created_at, published_at, attempts, last_error
            FROM outbox_event
            WHERE id = ?
            FOR UPDATE
            """;
    private static final String SQL_MARK_OUTBOX_PUBLISHED = """
            UPDATE outbox_event
            SET status = ?, published_at = ?, attempts = ?, next_attempt_at = NULL, last_error = NULL,
                claim_owner = NULL, claim_until = NULL
            WHERE id = ? AND (? IS NULL OR claim_owner = ?)
            """;
    private static final String SQL_MARK_OUTBOX_FAILURE = """
            UPDATE outbox_event
            SET status = ?, attempts = ?, next_attempt_at = ?, last_error = ?, published_at = NULL,
                claim_owner = NULL, claim_until = NULL
            WHERE id = ? AND (? IS NULL OR claim_owner = ?)
            """;

    private final DataSource dataSource;

    PostgresAtendimentoGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ClienteRecord criarCliente(String nome, String documento, String telefone, String email) {
        return criarClientePostgres(nome, documento, telefone, email);
    }

    @Override
    public List<ClienteRecord> listarClientes(ClienteSearchCriteria criteria) {
        return listarClientesPostgres(criteria);
    }

    @Override
    public ClienteRecord buscarCliente(UUID clienteId) {
        return buscarClientePostgres(clienteId);
    }

    @Override
    public ClienteRecord atualizarCliente(UUID clienteId, String nome, String documento, String telefone, String email) {
        return atualizarClientePostgres(clienteId, nome, documento, telefone, email);
    }

    @Override
    public VeiculoRecord criarVeiculo(UUID clienteId, String placa, String marca, String modelo, int ano) {
        return criarVeiculoPostgres(clienteId, placa, marca, modelo, ano);
    }

    @Override
    public List<VeiculoRecord> listarVeiculosDoCliente(UUID clienteId) {
        return listarVeiculosDoClientePostgres(clienteId);
    }

    @Override
    public VeiculoRecord buscarVeiculo(UUID veiculoId) {
        return buscarVeiculoPostgres(veiculoId);
    }

    @Override
    public VeiculoRecord atualizarVeiculo(UUID veiculoId, String placa, String marca, String modelo, int ano) {
        return atualizarVeiculoPostgres(veiculoId, placa, marca, modelo, ano);
    }

    @Override
    public OrdemServicoRecord criarOrdemServico(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        return criarOrdemServicoPostgres(clienteId, veiculoId, descricaoProblema);
    }

    @Override
    public List<OrdemServicoRecord> listarOrdensServico(TipoDeEstadoDaOrdemDeServico estado) {
        return listarOrdensServicoPostgres(estado);
    }

    @Override
    public OrdemServicoRecord buscarOrdemServico(UUID ordemServicoId) {
        return buscarOrdemServicoPostgres(ordemServicoId);
    }

    @Override
    public OrdemServicoRecord incluirServico(UUID ordemServicoId, ItemServicoRecord item, String correlationId) {
        return inTransaction(connection -> incluirServicoPostgres(connection, ordemServicoId, item, correlationId));
    }

    @Override
    public OrdemServicoRecord incluirPeca(UUID ordemServicoId, ItemPecaRecord item, String correlationId) {
        return inTransaction(connection -> incluirPecaPostgres(connection, ordemServicoId, item, correlationId));
    }

    @Override
    public List<HistoricoRecord> historico(UUID ordemServicoId) {
        return historicoPostgres(ordemServicoId);
    }

    @Override
    public OrdemServicoRecord alterarEstado(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        return alterarEstadoPostgres(ordemServicoId, novoEstado, motivo);
    }

    @Override
    public OperacaoAssincronaRecord cancelar(UUID ordemServicoId, String motivo) {
        return cancelarPostgres(ordemServicoId, motivo);
    }

    @Override
    public SagaRecord buscarSaga(UUID ordemServicoId) {
        return buscarSagaPostgres(ordemServicoId);
    }

    @Override
    public List<SagaHistoricoRecord> historicoSaga(UUID ordemServicoId) {
        return historicoSagaPostgres(ordemServicoId);
    }

    @Override
    public List<OutboxEventRecord> listarOutbox() {
        return listarOutboxPostgres();
    }

    @Override
    public List<OutboxEventRecord> publicarEventosPendentes() {
        return publicarEventosPendentesPostgres();
    }

    @Override
    public List<OutboxEventRecord> listarEventosPendentesParaPublicacao(int limit) {
        return listarEventosPendentesParaPublicacaoPostgres(limit);
    }

    @Override
    public List<OutboxEventRecord> reivindicarEventosPendentes(
            int limit, String claimOwner, OffsetDateTime claimUntil) {
        return inTransaction(connection -> {
            var now = OffsetDateTime.now(ZoneOffset.UTC);
            try (var statement = connection.prepareStatement(SQL_CLAIM_PENDING_OUTBOX)) {
                statement.setString(1, STATUS_PENDING);
                statement.setObject(2, now);
                statement.setObject(3, now);
                statement.setInt(4, Math.max(1, limit));
                statement.setString(5, claimOwner);
                statement.setObject(6, claimUntil);
                try (var resultSet = statement.executeQuery()) {
                    var claimed = new ArrayList<OutboxEventRecord>();
                    while (resultSet.next()) {
                        claimed.add(toOutboxEvent(resultSet));
                    }
                    return List.copyOf(claimed);
                }
            }
        });
    }

    @Override
    public OutboxEventRecord marcarEventoPublicado(UUID eventId) {
        return marcarEventoPublicadoPostgres(eventId);
    }

    @Override
    public OutboxEventRecord marcarEventoPublicado(UUID eventId, String claimOwner) {
        return marcarEventoPublicadoPostgres(eventId, claimOwner);
    }

    @Override
    public OutboxEventRecord marcarFalhaPublicacao(UUID eventId, String lastError, OffsetDateTime nextAttemptAt, boolean failed) {
        return marcarFalhaPublicacaoPostgres(eventId, lastError, nextAttemptAt, failed);
    }

    @Override
    public OutboxEventRecord marcarFalhaPublicacao(
            UUID eventId, String lastError, OffsetDateTime nextAttemptAt, boolean failed, String claimOwner) {
        return marcarFalhaPublicacaoPostgres(eventId, lastError, nextAttemptAt, failed, claimOwner);
    }

    @Override
    public SagaRecord consumirEvento(DomainEventEnvelope event) {
        return consumirEventoPostgres(event);
    }

    private ClienteRecord criarClientePostgres(String nome, String documento, String telefone, String email) {
        validarCliente(nome, documento, email);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var clienteId = UUID.randomUUID();
        var pessoaId = UUID.randomUUID();
        var documentoNormalizado = documento.trim();
        return inTransaction(connection -> {
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
            return new ClienteRecord(clienteId, nome.trim(), documentoNormalizado, normalizar(telefone), normalizar(email), agora, agora);
        });
    }

    private List<ClienteRecord> listarClientesPostgres(ClienteSearchCriteria criteria) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        SELECT c.id AS cliente_id, p.nome, p.documento, c.telefone, c.email, c.criado_em, c.atualizado_em
                        FROM cliente c
                        JOIN pessoa p ON p.id = c.pessoa_id
                        WHERE (? IS NULL OR LOWER(p.nome) LIKE ?)
                          AND (? IS NULL OR p.documento = ?)
                          AND (? IS NULL OR LOWER(c.email) LIKE ?)
                        ORDER BY c.criado_em
                        """)) {
            var nome = criteria.nome() == null ? null : "%" + criteria.nome().toLowerCase() + "%";
            var email = criteria.email() == null ? null : "%" + criteria.email().toLowerCase() + "%";
            statement.setString(1, nome);
            statement.setString(2, nome);
            statement.setString(3, criteria.documento());
            statement.setString(4, criteria.documento());
            statement.setString(5, email);
            statement.setString(6, email);
            try (var resultSet = statement.executeQuery()) {
            var result = new ArrayList<ClienteRecord>();
            while (resultSet.next()) {
                result.add(toCliente(resultSet));
            }
            return List.copyOf(result);
            }
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
        return inTransaction(connection -> {
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
            return buscarClientePostgres(connection, clienteId);
        });
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
                agora,
                List.of(),
                List.of(),
                EstadoSaga.INICIADA);
        return inTransaction(connection -> {
            var cliente = buscarClientePostgres(connection, clienteId);
            var veiculo = buscarVeiculoPostgres(connection, veiculoId);
            if (!veiculo.clienteId().equals(clienteId)) {
                throw new WebApplicationException("Veiculo nao pertence ao cliente informado.", Response.Status.CONFLICT);
            }
            inserirOrdemServico(connection, ordem);
            inserirHistorico(connection, ordem.ordemServicoId(), ordem.estado(), agora, "Ordem de servico recebida");
            var correlationId = correlationId(null);
            criarSagaInicialPostgres(connection, ordem.ordemServicoId(), agora, correlationId);
            var eventoPayload = new LinkedHashMap<String, Object>();
            eventoPayload.put(PAYLOAD_ORDEM_SERVICO_ID, ordem.ordemServicoId().toString());
            eventoPayload.put("clienteId", clienteId.toString());
            eventoPayload.put("veiculoId", veiculoId.toString());
            eventoPayload.put(PAYLOAD_ESTADO_ATUAL, ordem.estado().name());
            eventoPayload.put("criadoEm", agora.toString());
            eventoPayload.put("descricaoProblema", ordem.descricaoProblema());
            if (cliente.email() != null && !cliente.email().isBlank()) {
                eventoPayload.put("clienteEmail", cliente.email());
            }
            enfileirarEventoPostgres(
                    connection,
                    EVENT_ORDEM_DE_SERVICO_CRIADA,
                    "oficina.os.ordem-de-servico-criada",
                    ordem.ordemServicoId(),
                    eventoPayload,
                    correlationId,
                    agora);
            return ordem;
        });
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
                    var ordem = toOrdemServico(resultSet);
                    var saga = buscarSagaPostgres(connection, ordem.ordemServicoId());
                    result.add(new OrdemServicoRecord(
                            ordem.ordemServicoId(), ordem.clienteId(), ordem.veiculoId(), ordem.descricaoProblema(),
                            ordem.estado(), ordem.criadoEm(), ordem.atualizadoEm(), ordem.servicos(), ordem.pecas(),
                            saga == null ? null : saga.estado()));
                }
                var composed = new ArrayList<OrdemServicoRecord>();
                for (var ordem : result) {
                    composed.add(carregarComposicao(connection, ordem));
                }
                return List.copyOf(composed);
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
                            resultSet.getString(PAYLOAD_MOTIVO)));
                }
                return List.copyOf(result);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private OrdemServicoRecord incluirServicoPostgres(Connection connection, UUID ordemServicoId, ItemServicoRecord item,
            String correlationId) throws SQLException {
        var atual = validarComposicao(connection, ordemServicoId);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        try (var statement = connection.prepareStatement("""
                INSERT INTO ordem_servico_servico
                    (ordem_servico_id, servico_id, nome, quantidade, valor_unitario, criado_em)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, ordemServicoId);
            statement.setObject(2, item.servicoId());
            statement.setString(3, item.nome());
            statement.setBigDecimal(4, item.quantidade());
            statement.setBigDecimal(5, item.valorUnitario());
            statement.setObject(6, agora);
            statement.executeUpdate();
        }
        atualizarOrdem(connection, ordemServicoId, agora);
        enfileirarEventoPostgres(connection, "servicoIncluidoNaOrdemDeServico",
                "oficina.os.servico-incluido-na-ordem-de-servico", ordemServicoId,
                Map.of(PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId.toString(), "servico", Map.of(
                        "servicoId", item.servicoId().toString(), "nome", item.nome(), "quantidade", item.quantidade(),
                        "valorUnitario", item.valorUnitario(), "valorTotal", item.valorTotal()), "incluidoEm", agora.toString()),
                correlationId, agora);
        return carregarComposicao(connection, new OrdemServicoRecord(atual.ordemServicoId(), atual.clienteId(), atual.veiculoId(),
                atual.descricaoProblema(), atual.estado(), atual.criadoEm(), agora, List.of(), List.of(), atual.estadoSaga()));
    }

    private OrdemServicoRecord incluirPecaPostgres(Connection connection, UUID ordemServicoId, ItemPecaRecord item,
            String correlationId) throws SQLException {
        var atual = validarComposicao(connection, ordemServicoId);
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        try (var statement = connection.prepareStatement("""
                INSERT INTO ordem_servico_peca
                    (ordem_servico_id, peca_id, nome, quantidade, valor_unitario, criado_em)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setObject(1, ordemServicoId);
            statement.setObject(2, item.pecaId());
            statement.setString(3, item.nome());
            statement.setBigDecimal(4, item.quantidade());
            statement.setBigDecimal(5, item.valorUnitario());
            statement.setObject(6, agora);
            statement.executeUpdate();
        }
        atualizarOrdem(connection, ordemServicoId, agora);
        enfileirarEventoPostgres(connection, "pecaIncluidaNaOrdemDeServico",
                "oficina.os.peca-incluida-na-ordem-de-servico", ordemServicoId,
                Map.of(PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId.toString(), "peca", Map.of(
                        "pecaId", item.pecaId().toString(), "nome", item.nome(), "quantidade", item.quantidade(),
                        "valorUnitario", item.valorUnitario(), "valorTotal", item.valorTotal()), "incluidaEm", agora.toString()),
                correlationId, agora);
        return carregarComposicao(connection, new OrdemServicoRecord(atual.ordemServicoId(), atual.clienteId(), atual.veiculoId(),
                atual.descricaoProblema(), atual.estado(), atual.criadoEm(), agora, List.of(), List.of(), atual.estadoSaga()));
    }

    private OrdemServicoRecord validarComposicao(Connection connection, UUID ordemServicoId) throws SQLException {
        var ordem = buscarOrdemServicoPostgres(connection, ordemServicoId);
        if (ordem.estado() != TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO) {
            throw new WebApplicationException("Itens so podem ser incluidos durante o diagnostico.", Response.Status.CONFLICT);
        }
        return ordem;
    }

    private void atualizarOrdem(Connection connection, UUID ordemServicoId, OffsetDateTime agora) throws SQLException {
        try (var statement = connection.prepareStatement("UPDATE ordem_de_servico SET atualizado_em = ? WHERE id = ?")) {
            statement.setObject(1, agora);
            statement.setObject(2, ordemServicoId);
            statement.executeUpdate();
        }
    }

    private OrdemServicoRecord alterarEstadoPostgres(UUID ordemServicoId, TipoDeEstadoDaOrdemDeServico novoEstado, String motivo) {
        return inTransaction(connection -> alterarEstadoPostgres(connection, ordemServicoId, novoEstado, motivo, true));
    }

    private OperacaoAssincronaRecord cancelarPostgres(UUID ordemServicoId, String motivo) {
        return inTransaction(connection -> {
            buscarOrdemServicoPostgres(connection, ordemServicoId);
            compensarSagaPostgres(connection, ordemServicoId, normalizar(motivo));
            return new OperacaoAssincronaRecord("ACEITO", OffsetDateTime.now(ZoneOffset.UTC));
        });
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
                var statement = connection.prepareStatement(SQL_SELECT_OUTBOX);
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
        return listarEventosPendentesParaPublicacaoPostgres(Integer.MAX_VALUE).stream()
                .map(event -> marcarEventoPublicadoPostgres(event.eventId()))
                .toList();
    }

    private List<OutboxEventRecord> listarEventosPendentesParaPublicacaoPostgres(int limit) {
        var effectiveLimit = Math.max(1, limit);
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(SQL_SELECT_PENDING_OUTBOX_FOR_PUBLICATION)) {
            statement.setString(1, STATUS_PENDING);
            statement.setObject(2, OffsetDateTime.now(ZoneOffset.UTC));
            statement.setInt(3, effectiveLimit);
            try (var resultSet = statement.executeQuery()) {
                var result = new ArrayList<OutboxEventRecord>();
                while (resultSet.next()) {
                    result.add(toOutboxEvent(resultSet));
                }
                return List.copyOf(result);
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    private OutboxEventRecord marcarEventoPublicadoPostgres(UUID eventId) {
        return marcarEventoPublicadoPostgres(eventId, null);
    }

    private OutboxEventRecord marcarEventoPublicadoPostgres(UUID eventId, String claimOwner) {
        var publicado = inTransaction(connection -> {
            var event = buscarOutboxPorIdParaUpdate(connection, eventId);
            var agora = OffsetDateTime.now(ZoneOffset.UTC);
            var updated = new OutboxEventRecord(
                    event.eventId(),
                    event.aggregateId(),
                    event.eventType(),
                    event.eventVersion(),
                    event.topic(),
                    event.producer(),
                    event.payload(),
                    STATUS_PUBLISHED,
                    event.correlationId(),
                    event.occurredAt(),
                    event.createdAt(),
                    agora,
                    event.attempts() + 1,
                    null);
            try (var statement = connection.prepareStatement(SQL_MARK_OUTBOX_PUBLISHED)) {
                statement.setString(1, updated.status());
                statement.setObject(2, updated.publishedAt());
                statement.setInt(3, updated.attempts());
                statement.setObject(4, updated.eventId());
                statement.setString(5, claimOwner);
                statement.setString(6, claimOwner);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("Claim da Outbox nao pertence a esta replica: " + eventId);
                }
            }
            return updated;
        });
        logEvent(LOG, "outbox event published", publicado, STATUS_PUBLISHED);
        return publicado;
    }

    private OutboxEventRecord marcarFalhaPublicacaoPostgres(
            UUID eventId,
            String lastError,
            OffsetDateTime nextAttemptAt,
            boolean failed) {
        return marcarFalhaPublicacaoPostgres(eventId, lastError, nextAttemptAt, failed, null);
    }

    private OutboxEventRecord marcarFalhaPublicacaoPostgres(
            UUID eventId,
            String lastError,
            OffsetDateTime nextAttemptAt,
            boolean failed,
            String claimOwner) {
        var status = failed ? STATUS_FAILED : STATUS_PENDING;
        var updated = inTransaction(connection -> {
            var event = buscarOutboxPorIdParaUpdate(connection, eventId);
            var failure = new OutboxEventRecord(
                    event.eventId(),
                    event.aggregateId(),
                    event.eventType(),
                    event.eventVersion(),
                    event.topic(),
                    event.producer(),
                    event.payload(),
                    status,
                    event.correlationId(),
                    event.occurredAt(),
                    event.createdAt(),
                    null,
                    event.attempts() + 1,
                    lastError);
            try (var statement = connection.prepareStatement(SQL_MARK_OUTBOX_FAILURE)) {
                statement.setString(1, failure.status());
                statement.setInt(2, failure.attempts());
                statement.setObject(3, failed ? null : nextAttemptAt);
                statement.setString(4, failure.lastError());
                statement.setObject(5, failure.eventId());
                statement.setString(6, claimOwner);
                statement.setString(7, claimOwner);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("Claim da Outbox nao pertence a esta replica: " + eventId);
                }
            }
            return failure;
        });
        logEvent(LOG, "outbox event publication failed", updated, status);
        return updated;
    }

    private OutboxEventRecord buscarOutboxPorIdParaUpdate(Connection connection, UUID eventId) throws SQLException {
        try (var statement = connection.prepareStatement(SQL_SELECT_OUTBOX_BY_ID_FOR_UPDATE)) {
            statement.setObject(1, eventId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return toOutboxEvent(resultSet);
                }
                throw new IllegalStateException("Evento de Outbox nao encontrado: " + eventId);
            }
        }
    }

    private SagaRecord consumirEventoPostgres(DomainEventEnvelope event) {
        var ordemServicoId = uuidFromPayload(event.payload(), PAYLOAD_ORDEM_SERVICO_ID, event.aggregateId());
        return inTransaction(connection -> consumirEventoPostgres(connection, event, ordemServicoId));
    }

    private SagaRecord consumirEventoPostgres(Connection connection, DomainEventEnvelope event, UUID ordemServicoId) throws SQLException {
        if (inboxContem(connection, event.eventId())) {
            var saga = buscarSagaPostgres(connection, ordemServicoId);
            logEvent(LOG, "domain event ignored", event, "DUPLICATE", ordemServicoId, correlationId(saga, event));
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
        logEvent(LOG, "domain event consumed", event, "CONSUMED", ordemServicoId, correlationId(resultado, event));
        return resultado;
    }

    private OrdemServicoRecord alterarEstadoPostgres(
            Connection connection,
            UUID ordemServicoId,
            TipoDeEstadoDaOrdemDeServico novoEstado,
            String motivo,
            boolean finalizarEntrega) throws SQLException {
        return alterarEstadoPostgres(connection, ordemServicoId, novoEstado, motivo, finalizarEntrega, false);
    }

    private OrdemServicoRecord alterarEstadoPostgres(
            Connection connection,
            UUID ordemServicoId,
            TipoDeEstadoDaOrdemDeServico novoEstado,
            String motivo,
            boolean finalizarEntrega,
            boolean originadaPorEvento) throws SQLException {
        var atual = buscarOrdemServicoPostgres(connection, ordemServicoId);
        if (originadaPorEvento) {
            AtendimentoGatewaySupport.validarTransicaoPorEvento(atual.estado(), novoEstado);
        } else {
            validarTransicao(atual.estado(), novoEstado);
            var saga = buscarSagaPostgres(connection, ordemServicoId);
            AtendimentoGatewaySupport.validarEntregaLiberada(saga == null ? null : saga.estado());
        }
        var atualizado = new OrdemServicoRecord(
                atual.ordemServicoId(),
                atual.clienteId(),
                atual.veiculoId(),
                atual.descricaoProblema(),
                novoEstado,
                atual.criadoEm(),
                OffsetDateTime.now(ZoneOffset.UTC),
                atual.servicos(),
                atual.pecas(),
                atual.estadoSaga());
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
        if (saga.estado() != EstadoSaga.INICIADA && saga.estado() != EstadoSaga.EM_DIAGNOSTICO) {
            logEvent(LOG, "domain event ignored", event, "INVALID_STATE", saga.ordemServicoId(), correlationId(saga, event));
            return saga;
        }
        var ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.RECEBIDA) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, "Diagnostico iniciado", false, true);
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
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.RECEBIDA) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO,
                    "Diagnostico iniciado por evento finalizado", false, true);
            ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        }
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO, "Diagnostico finalizado", false, true);
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
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, "Orcamento recusado", false, true);
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
        if (saga.estado() != EstadoSaga.AGUARDANDO_APROVACAO && saga.estado() != EstadoSaga.EM_EXECUCAO) {
            logEvent(LOG, "domain event ignored", event, "INVALID_STATE", saga.ordemServicoId(), correlationId(saga, event));
            return saga;
        }
        var ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, "Execucao iniciada", false, true);
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
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO) {
            alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO,
                    "Execucao iniciada por evento finalizado", false, true);
            ordem = buscarOrdemServicoPostgres(connection, saga.ordemServicoId());
        }
        if (ordem.estado() == TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO) {
            ordem = alterarEstadoPostgres(connection, ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.FINALIZADA, "Execucao finalizada", false, true);
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
                STATUS_PENDING,
                effectiveCorrelationId,
                occurredAt,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                0,
                null);
        PostgresPersistenceSupport.insertOutbox(connection, event);
        logEvent(LOG, "outbox event registered", event, STATUS_PENDING);
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
                return carregarComposicao(connection, toOrdemServico(resultSet));
            }
        }
    }

    private OrdemServicoRecord carregarComposicao(Connection connection, OrdemServicoRecord ordem) throws SQLException {
        var servicos = new ArrayList<ItemServicoRecord>();
        try (var statement = connection.prepareStatement("""
                SELECT servico_id, nome, quantidade, valor_unitario
                FROM ordem_servico_servico WHERE ordem_servico_id = ? ORDER BY criado_em
                """)) {
            statement.setObject(1, ordem.ordemServicoId());
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    servicos.add(new ItemServicoRecord(uuid(resultSet, "servico_id"), resultSet.getString("nome"),
                            resultSet.getBigDecimal("quantidade"), resultSet.getBigDecimal("valor_unitario")));
                }
            }
        }
        var pecas = new ArrayList<ItemPecaRecord>();
        try (var statement = connection.prepareStatement("""
                SELECT peca_id, nome, quantidade, valor_unitario
                FROM ordem_servico_peca WHERE ordem_servico_id = ? ORDER BY criado_em
                """)) {
            statement.setObject(1, ordem.ordemServicoId());
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    pecas.add(new ItemPecaRecord(uuid(resultSet, "peca_id"), resultSet.getString("nome"),
                            resultSet.getBigDecimal("quantidade"), resultSet.getBigDecimal("valor_unitario")));
                }
            }
        }
        var saga = buscarSagaPostgres(connection, ordem.ordemServicoId());
        return new OrdemServicoRecord(ordem.ordemServicoId(), ordem.clienteId(), ordem.veiculoId(), ordem.descricaoProblema(),
                ordem.estado(), ordem.criadoEm(), ordem.atualizadoEm(), servicos, pecas,
                saga == null ? ordem.estadoSaga() : saga.estado());
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
                uuid(resultSet, COLUMN_CLIENTE_ID),
                resultSet.getString("nome"),
                resultSet.getString("documento"),
                resultSet.getString("telefone"),
                resultSet.getString("email"),
                offsetDateTime(resultSet, COLUMN_CRIADO_EM),
                offsetDateTime(resultSet, COLUMN_ATUALIZADO_EM));
    }

    private VeiculoRecord toVeiculo(ResultSet resultSet) throws SQLException {
        return new VeiculoRecord(
                uuid(resultSet, "id"),
                uuid(resultSet, COLUMN_CLIENTE_ID),
                resultSet.getString("placa"),
                resultSet.getString("marca"),
                resultSet.getString("modelo"),
                resultSet.getInt("ano"),
                offsetDateTime(resultSet, COLUMN_CRIADO_EM),
                offsetDateTime(resultSet, COLUMN_ATUALIZADO_EM));
    }

    private OrdemServicoRecord toOrdemServico(ResultSet resultSet) throws SQLException {
        return new OrdemServicoRecord(
                uuid(resultSet, "id"),
                uuid(resultSet, COLUMN_CLIENTE_ID),
                uuid(resultSet, "veiculo_id"),
                resultSet.getString("descricao_problema"),
                TipoDeEstadoDaOrdemDeServico.valueOf(resultSet.getString("estado_atual")),
                offsetDateTime(resultSet, COLUMN_CRIADO_EM),
                offsetDateTime(resultSet, COLUMN_ATUALIZADO_EM),
                List.of(),
                List.of(),
                null);
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
                offsetDateTime(resultSet, COLUMN_CRIADO_EM),
                offsetDateTime(resultSet, COLUMN_ATUALIZADO_EM),
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
                resultSet.getString(PAYLOAD_MOTIVO),
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

    private static RuntimeException persistenceFailure(SQLException exception) {
        if ("23505".equals(exception.getSQLState())) {
            return new WebApplicationException("Item ja incluido na ordem de servico.", Response.Status.CONFLICT);
        }
        return new IllegalStateException("Falha ao acessar PostgreSQL do oficina-os-service.", exception);
    }

    private <T> T inTransaction(PostgresPersistenceSupport.SqlOperation<T> operation) {
        try (var connection = dataSource.getConnection()) {
            return PostgresPersistenceSupport.executeTransaction(connection, operation);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }
}

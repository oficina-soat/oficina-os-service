package br.com.oficina.os.framework.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.entities.pessoa.Pessoa;
import br.com.oficina.os.core.entities.usuario.CpfOperacional;
import br.com.oficina.os.core.entities.usuario.TipoDePapel;
import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.core.entities.usuario.UsuarioStatus;
import br.com.oficina.os.core.exceptions.UsuarioConflitanteException;
import br.com.oficina.os.core.exceptions.UsuarioNaoEncontradoException;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ItemPecaRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ItemServicoRecord;
import br.com.oficina.os.framework.idempotency.IdempotencyRecord.ProcessingStatus;
import br.com.oficina.os.framework.idempotency.PersistentIdempotencyStore;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

@QuarkusTest
@TestProfile(PostgresAtendimentoSeedStoreTest.PostgresStoreProfile.class)
@QuarkusTestResource(value = PostgresAtendimentoSeedStoreTest.PostgresResource.class, restrictToAnnotatedClass = true)
class PostgresAtendimentoSeedStoreTest {
    @Inject
    AtendimentoSeedStore store;

    @Inject
    UsuarioStore usuarioStore;

    @Inject
    DataSource dataSource;

    @Test
    void devePersistirAtendimentoSagaInboxEOutboxEmPostgreSQL() {
        var cliente = store.criarCliente(
                "Cliente PostgreSQL",
                "54281867040",
                "+5511777777777",
                "postgresql@example.com");
        var veiculo = store.criarVeiculo(cliente.clienteId(), "pgs1t23", "Honda", "Civic", 2022);
        var ordem = store.criarOrdemServico(cliente.clienteId(), veiculo.veiculoId(), "Persistir OS em PostgreSQL");
        var abertura = store.listarOutbox().stream()
                .filter(event -> event.aggregateId().equals(ordem.ordemServicoId()))
                .filter(event -> event.eventType().equals("ordemDeServicoCriada"))
                .findFirst()
                .orElseThrow();
        assertEquals("postgresql@example.com", abertura.payload().get("clienteEmail"));

        var reloaded = new AtendimentoSeedStore(dataSource, "postgresql");
        assertEquals("Cliente PostgreSQL", reloaded.buscarCliente(cliente.clienteId()).nome());
        assertEquals("PGS1T23", reloaded.buscarVeiculo(veiculo.veiculoId()).placa());
        assertEquals("Persistir OS em PostgreSQL", reloaded.buscarOrdemServico(ordem.ordemServicoId()).descricaoProblema());
        assertEquals(EstadoSaga.INICIADA, reloaded.buscarSaga(ordem.ordemServicoId()).estado());
        assertTrue(reloaded.listarOutbox().stream()
                .anyMatch(event -> event.eventId().equals(abertura.eventId()) && event.status().equals("PENDING")));

        var publicados = reloaded.publicarEventosPendentes();
        assertTrue(publicados.stream().anyMatch(event -> event.eventId().equals(abertura.eventId())));
        assertTrue(new AtendimentoSeedStore(dataSource, "postgresql").listarOutbox().stream()
                .anyMatch(event -> event.eventId().equals(abertura.eventId()) && event.status().equals("PUBLISHED")));

        var evento = new DomainEventEnvelope(
                UUID.randomUUID(),
                "diagnosticoIniciado",
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "oficina-execution-service",
                ordem.ordemServicoId(),
                Map.of("ordemServicoId", ordem.ordemServicoId().toString()));
        reloaded.consumirEvento(evento);
        reloaded.consumirEvento(evento);

        var servicoId = UUID.randomUUID();
        var pecaId = UUID.randomUUID();
        reloaded.incluirServico(ordem.ordemServicoId(),
                new ItemServicoRecord(servicoId, "Diagnostico", BigDecimal.ONE, new BigDecimal("80.00")), "corr-items");
        reloaded.incluirPeca(ordem.ordemServicoId(),
                new ItemPecaRecord(pecaId, "Filtro", new BigDecimal("2"), new BigDecimal("25.00")), "corr-items");

        var afterInboxReload = new AtendimentoSeedStore(dataSource, "postgresql");
        assertEquals(EstadoSaga.EM_DIAGNOSTICO, afterInboxReload.buscarSaga(ordem.ordemServicoId()).estado());
        assertEquals(2, afterInboxReload.historicoSaga(ordem.ordemServicoId()).size());
        assertEquals(2, afterInboxReload.historico(ordem.ordemServicoId()).size());
        assertFalse(afterInboxReload.listarOutbox().isEmpty());
        assertEquals(servicoId, afterInboxReload.buscarOrdemServico(ordem.ordemServicoId()).servicos().getFirst().servicoId());
        assertEquals(pecaId, afterInboxReload.buscarOrdemServico(ordem.ordemServicoId()).pecas().getFirst().pecaId());
        assertTrue(afterInboxReload.listarOutbox().stream().anyMatch(event ->
                event.eventType().equals("servicoIncluidoNaOrdemDeServico") && event.correlationId().equals("corr-items")));
    }

    @Test
    void devePersistirRegistrosDeIdempotenciaNoPostgreSQL() {
        var idempotencyStore = new PersistentIdempotencyStore(dataSource);
        var idempotencyRecord = idempotencyStore.createProcessing(
                "oficina-os-service:POST:/api/v1/clientes:anonymous",
                "postgres-idempotency-001",
                "hash-postgres-001",
                "correlation-postgres-001",
                "request-postgres-001",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));

        assertEquals(ProcessingStatus.PROCESSING, idempotencyRecord.processingStatus());

        idempotencyStore.complete(
                idempotencyRecord.scope(),
                idempotencyRecord.key(),
                ProcessingStatus.COMPLETED,
                201,
                "{\"clienteId\":\"cliente-postgres-001\"}");

        var reloaded = new PersistentIdempotencyStore(dataSource)
                .find(idempotencyRecord.scope(), idempotencyRecord.key())
                .orElseThrow();
        assertEquals(ProcessingStatus.COMPLETED, reloaded.processingStatus());
        assertEquals(201, reloaded.responseStatus());
        assertEquals("{\"clienteId\":\"cliente-postgres-001\"}", reloaded.responseBody());
        assertEquals("correlation-postgres-001", reloaded.correlationId());
        assertEquals("request-postgres-001", reloaded.requestId());
    }

    @Test
    void devePersistirCrudDeUsuariosSemCredenciaisNoPostgreSQL() throws Exception {
        var pessoaSolicitadaId = UUID.randomUUID();
        var usuario = new Usuario(
                UUID.randomUUID(),
                new Pessoa(pessoaSolicitadaId, new CpfOperacional("50132372037"), "Cliente e Operador"),
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.MECANICO, TipoDePapel.RECEPCIONISTA));

        var criado = usuarioStore.criar(usuario);
        var pessoaSeedCliente = UUID.fromString("10000000-0000-4000-8000-000000000004");

        assertNotEquals(pessoaSolicitadaId, criado.pessoa().id());
        assertEquals(pessoaSeedCliente, criado.pessoa().id());
        assertEquals("Cliente e Operador", criado.pessoa().nome());
        assertTrue(usuarioStore.listar().stream().anyMatch(candidate -> candidate.id().equals(criado.id())));
        assertEquals(criado.id(), new UsuarioStore(dataSource, "postgresql").buscar(criado.id()).id());

        var atualizado = usuarioStore.atualizar(criado.atualizado(
                new Pessoa(criado.pessoa().id(), new CpfOperacional("52998224725"), "Operador Atualizado"),
                UsuarioStatus.BLOQUEADO,
                Set.of(TipoDePapel.ADMINISTRATIVO)));
        assertEquals("52998224725", atualizado.pessoa().documento().valor());
        assertEquals(UsuarioStatus.BLOQUEADO, atualizado.status());
        assertEquals(Set.of(TipoDePapel.ADMINISTRATIVO), atualizado.papeis());

        var novaPessoaId = UUID.randomUUID();
        var segundoUsuario = usuarioStore.criar(new Usuario(
                UUID.randomUUID(),
                new Pessoa(novaPessoaId, new CpfOperacional("11144477735"), "Novo Operador"),
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.RECEPCIONISTA)));
        assertEquals(novaPessoaId, segundoUsuario.pessoa().id());

        usuarioStore.inativar(criado.id());
        usuarioStore.inativar(criado.id());
        assertEquals(UsuarioStatus.INATIVO, usuarioStore.buscar(criado.id()).status());

        assertEventosDoUsuario(criado);

        var administradorDuplicado = new Usuario(
                UUID.randomUUID(),
                new Pessoa(UUID.randomUUID(), new CpfOperacional("84191404067"), "Administrador Duplicado"),
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.ADMINISTRATIVO));
        assertThrows(UsuarioConflitanteException.class, () -> usuarioStore.criar(administradorDuplicado));
        var usuarioComIdDuplicado = new Usuario(
                UsuarioStore.SEED_ADMIN_ID,
                new Pessoa(UUID.randomUUID(), new CpfOperacional("12345678901"), "ID Duplicado"),
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.ADMINISTRATIVO));
        assertThrows(UsuarioConflitanteException.class, () -> usuarioStore.criar(usuarioComIdDuplicado));
        var usuarioComCpfConflitante = atualizado.atualizado(
                new Pessoa(atualizado.pessoa().id(), new CpfOperacional("36655462007"), "CPF Conflitante"),
                UsuarioStatus.ATIVO,
                Set.of(TipoDePapel.MECANICO));
        assertThrows(UsuarioConflitanteException.class, () -> usuarioStore.atualizar(usuarioComCpfConflitante));
        var usuarioInexistenteId = UUID.randomUUID();
        assertThrows(UsuarioNaoEncontradoException.class, () -> usuarioStore.buscar(usuarioInexistenteId));

        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        SELECT count(*)
                        FROM information_schema.columns
                        WHERE table_name = 'usuario' AND column_name = 'password_hash'
                        """);
                var resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next());
            assertEquals(0, resultSet.getInt(1));
        }
    }

    private void assertEventosDoUsuario(Usuario criado) {
        var eventosDoUsuario = store.listarOutbox().stream()
                .filter(event -> event.aggregateId().equals(criado.id()))
                .toList();
        assertEquals(3, eventosDoUsuario.size());
        assertEquals(1, eventosDoUsuario.stream().filter(event -> event.eventType().equals("usuarioAdicionado")).count());
        assertEquals(1, eventosDoUsuario.stream().filter(event -> event.eventType().equals("usuarioAtualizado")).count());
        assertEquals(1, eventosDoUsuario.stream().filter(event -> event.eventType().equals("usuarioExcluido")).count());
        assertTrue(eventosDoUsuario.stream().allMatch(event -> event.producer().equals("oficina-os-service")));
        assertTrue(eventosDoUsuario.stream().allMatch(event -> event.payload().get("usuarioId").equals(criado.id().toString())));
        assertTrue(eventosDoUsuario.stream().noneMatch(event -> event.payload().containsKey("password")));
        var exclusao = eventosDoUsuario.stream()
                .filter(event -> event.eventType().equals("usuarioExcluido"))
                .findFirst()
                .orElseThrow();
        assertEquals("oficina.os.usuario-excluido", exclusao.topic());
        assertEquals("INATIVO", exclusao.payload().get("status"));
        assertEquals("52998224725", exclusao.payload().get("documento"));
        assertEquals(List.of("administrativo"), exclusao.payload().get("papeis"));
    }

    @Test
    void deveCobrirOperacoesPostgreSQLDeConsultaAtualizacaoEFluxosDaSaga() {
        var cliente = store.criarCliente(
                "Cliente Fluxo PostgreSQL",
                "04252011000110",
                "+5511888888888",
                "fluxo@example.com");
        var clienteAtualizado = store.atualizarCliente(
                cliente.clienteId(),
                "Cliente Fluxo Atualizado",
                "04252011000110",
                "+5511888880000",
                "fluxo.atualizado@example.com");
        assertEquals("Cliente Fluxo Atualizado", clienteAtualizado.nome());
        assertTrue(store.listarClientes().stream().anyMatch(candidate -> candidate.clienteId().equals(cliente.clienteId())));

        var veiculo = store.criarVeiculo(cliente.clienteId(), "flx1a23", "Toyota", "Corolla", 2023);
        var veiculoAtualizado = store.atualizarVeiculo(veiculo.veiculoId(), "flx9z87", "Toyota", "Corolla Cross", 2024);
        assertEquals("FLX9Z87", veiculoAtualizado.placa());
        assertTrue(store.listarVeiculosDoCliente(cliente.clienteId()).stream()
                .anyMatch(candidate -> candidate.veiculoId().equals(veiculo.veiculoId())));

        var ordemFeliz = store.criarOrdemServico(cliente.clienteId(), veiculo.veiculoId(), "Fluxo feliz PostgreSQL");
        var execucaoId = UUID.randomUUID();
        var orcamentoId = UUID.randomUUID();
        var pagamentoId = UUID.randomUUID();

        store.consumirEvento(evento("diagnosticoIniciado", ordemFeliz.ordemServicoId(), Map.of("execucaoId", execucaoId.toString())));
        store.consumirEvento(evento("diagnosticoFinalizado", ordemFeliz.ordemServicoId(), Map.of("execucaoId", execucaoId.toString())));
        store.consumirEvento(evento("orcamentoGerado", ordemFeliz.ordemServicoId(), Map.of("orcamentoId", orcamentoId.toString())));
        store.consumirEvento(evento("orcamentoAprovado", ordemFeliz.ordemServicoId(), Map.of("orcamentoId", orcamentoId.toString())));
        store.consumirEvento(evento("execucaoIniciada", ordemFeliz.ordemServicoId(), Map.of("execucaoId", execucaoId.toString())));
        store.consumirEvento(evento("execucaoFinalizada", ordemFeliz.ordemServicoId(), Map.of("execucaoId", execucaoId.toString())));
        store.consumirEvento(evento("pagamentoSolicitado", ordemFeliz.ordemServicoId(), Map.of(
                "orcamentoId", orcamentoId.toString(),
                "pagamentoId", pagamentoId.toString())));
        store.consumirEvento(evento("pagamentoConfirmado", ordemFeliz.ordemServicoId(), Map.of("pagamentoId", pagamentoId.toString())));
        store.alterarEstado(ordemFeliz.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.ENTREGUE, "Cliente retirou");

        var sagaFeliz = new AtendimentoSeedStore(dataSource, "postgresql").buscarSaga(ordemFeliz.ordemServicoId());
        assertEquals(EstadoSaga.FINALIZADA_COM_SUCESSO, sagaFeliz.estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.ENTREGUE, store.buscarOrdemServico(ordemFeliz.ordemServicoId()).estado());
        assertTrue(store.listarOrdensServico(TipoDeEstadoDaOrdemDeServico.ENTREGUE).stream()
                .anyMatch(candidate -> candidate.ordemServicoId().equals(ordemFeliz.ordemServicoId())));
        assertTrue(store.listarOutbox().stream()
                .anyMatch(event -> event.aggregateId().equals(ordemFeliz.ordemServicoId())
                        && event.eventType().equals("sagaFinalizadaComSucesso")));

        var ordemRecusada = store.criarOrdemServico(cliente.clienteId(), veiculo.veiculoId(), "Orcamento recusado PostgreSQL");
        var orcamentoRecusadoId = UUID.randomUUID();
        store.consumirEvento(evento("diagnosticoIniciado", ordemRecusada.ordemServicoId(), Map.of()));
        store.consumirEvento(evento("diagnosticoFinalizado", ordemRecusada.ordemServicoId(), Map.of()));
        store.consumirEvento(evento("orcamentoGerado", ordemRecusada.ordemServicoId(), Map.of("orcamentoId", orcamentoRecusadoId.toString())));
        store.consumirEvento(evento("orcamentoRecusado", ordemRecusada.ordemServicoId(), Map.of(
                "orcamentoId", orcamentoRecusadoId.toString(),
                "motivo", "Cliente pediu revisao")));
        assertEquals(EstadoSaga.EM_DIAGNOSTICO, store.buscarSaga(ordemRecusada.ordemServicoId()).estado());

        var ordemCancelada = store.criarOrdemServico(cliente.clienteId(), veiculo.veiculoId(), "Cancelamento PostgreSQL");
        store.cancelar(ordemCancelada.ordemServicoId(), "Cliente cancelou");
        assertEquals(EstadoSaga.COMPENSADA, store.buscarSaga(ordemCancelada.ordemServicoId()).estado());

        var clienteInexistente = UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa");
        var veiculoInexistente = UUID.fromString("bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb");
        var ordemInexistente = UUID.fromString("cccccccc-cccc-4ccc-cccc-cccccccccccc");
        assertThrows(NotFoundException.class, () -> store.buscarCliente(clienteInexistente));
        assertThrows(NotFoundException.class, () -> store.buscarVeiculo(veiculoInexistente));
        assertThrows(NotFoundException.class, () -> store.buscarOrdemServico(ordemInexistente));
    }

    @Test
    void deveIgnorarDiagnosticoIniciadoAtrasadoNoPostgreSQL() {
        var ordem = store.criarOrdemServico(
                AtendimentoGateway.SEED_CLIENTE_ID,
                AtendimentoGateway.SEED_VEICULO_ID,
                "Eventos de diagnóstico fora de ordem PostgreSQL");
        var execucaoId = UUID.randomUUID();
        var sagaInicial = store.buscarSaga(ordem.ordemServicoId());
        var diagnosticoIniciadoEm = sagaInicial.atualizadoEm().plusSeconds(1);
        var diagnosticoFinalizadoEm = diagnosticoIniciadoEm.plusSeconds(1);

        store.consumirEvento(evento("diagnosticoFinalizado", ordem.ordemServicoId(),
                Map.of("execucaoId", execucaoId.toString()), diagnosticoFinalizadoEm));
        var saga = store.consumirEvento(evento("diagnosticoIniciado", ordem.ordemServicoId(),
                Map.of("execucaoId", execucaoId.toString()), diagnosticoIniciadoEm));

        assertEquals(EstadoSaga.AGUARDANDO_ORCAMENTO, saga.estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO,
                store.buscarOrdemServico(ordem.ordemServicoId()).estado());
    }

    private static DomainEventEnvelope evento(String eventType, UUID ordemServicoId, Map<String, Object> payload) {
        return evento(eventType, ordemServicoId, payload, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static DomainEventEnvelope evento(
            String eventType,
            UUID ordemServicoId,
            Map<String, Object> payload,
            OffsetDateTime occurredAt) {
        var completePayload = new java.util.LinkedHashMap<String, Object>();
        completePayload.put("ordemServicoId", ordemServicoId.toString());
        completePayload.putAll(payload);
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                occurredAt,
                producer(eventType),
                ordemServicoId,
                completePayload);
    }

    private static String producer(String eventType) {
        return switch (eventType) {
            case "diagnosticoIniciado", "diagnosticoFinalizado", "execucaoIniciada", "execucaoFinalizada" ->
                    "oficina-execution-service";
            default -> "oficina-billing-service";
        };
    }

    public static class PostgresStoreProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("oficina.persistence.kind", "postgresql"),
                    Map.entry("quarkus.datasource.active", "true"),
                    Map.entry("quarkus.datasource.db-kind", "postgresql"),
                    Map.entry("quarkus.datasource.devservices.enabled", "false"),
                    Map.entry("quarkus.flyway.active", "true"),
                    Map.entry("quarkus.flyway.migrate-at-start", "true"),
                    Map.entry("quarkus.hibernate-orm.active", "false"),
                    Map.entry("quarkus.log.console.json.enabled", "false"),
                    Map.entry("quarkus.otel.traces.enabled", "false"));
        }
    }

    public static class PostgresResource implements QuarkusTestResourceLifecycleManager {
        private PostgreSQLContainer postgres;

        @Override
        public Map<String, String> start() {
            postgres = new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("oficina_os")
                    .withUsername("oficina_os_user")
                    .withPassword("oficina_os_password");
            postgres.start();
            return Map.of(
                    "quarkus.datasource.jdbc.url", postgres.getJdbcUrl(),
                    "quarkus.datasource.username", postgres.getUsername(),
                    "quarkus.datasource.password", postgres.getPassword());
        }

        @Override
        public void stop() {
            if (postgres != null) {
                postgres.stop();
            }
        }
    }
}

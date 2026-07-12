package br.com.oficina.os.framework.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
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
import java.time.ZoneOffset;
import java.util.Map;
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

        var afterInboxReload = new AtendimentoSeedStore(dataSource, "postgresql");
        assertEquals(EstadoSaga.EM_DIAGNOSTICO, afterInboxReload.buscarSaga(ordem.ordemServicoId()).estado());
        assertEquals(2, afterInboxReload.historicoSaga(ordem.ordemServicoId()).size());
        assertEquals(2, afterInboxReload.historico(ordem.ordemServicoId()).size());
        assertFalse(afterInboxReload.listarOutbox().isEmpty());
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

    private static DomainEventEnvelope evento(String eventType, UUID ordemServicoId, Map<String, Object> payload) {
        var completePayload = new java.util.LinkedHashMap<String, Object>();
        completePayload.put("ordemServicoId", ordemServicoId.toString());
        completePayload.putAll(payload);
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
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

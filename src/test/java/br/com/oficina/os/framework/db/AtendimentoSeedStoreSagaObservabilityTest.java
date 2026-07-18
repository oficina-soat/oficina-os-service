package br.com.oficina.os.framework.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.framework.observability.OperationalMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtendimentoSeedStoreSagaObservabilityTest {
    private SimpleMeterRegistry registry;
    private AtendimentoSeedStore store;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        store = new AtendimentoSeedStore(
                new InMemoryAtendimentoGateway(),
                new OperationalMetrics(registry, "oficina-os-service"),
                "memory");
    }

    @Test
    void deveInstrumentarInicioTransicaoEIgnorarEventoDuplicado() {
        var ordem = store.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                "Instrumentar saga");
        var event = evento("diagnosticoIniciado", ordem.ordemServicoId());

        store.consumirEvento(event);
        store.consumirEvento(event);

        assertEquals(1, registry.get("saga.instances.started.count").counter().count());
        assertEquals(1, registry.get("saga.step.duration")
                .tag("step", "diagnosticoIniciado")
                .timer()
                .count());
    }

    @Test
    void deveInstrumentarCompensacaoComMotivoCategorizado() {
        var ordem = store.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                "Compensar saga");

        store.cancelar(ordem.ordemServicoId(), "Falha operacional na execucao");

        assertEquals(1, registry.get("saga.instances.compensated.count")
                .tags("sagaType", "ordemServico", "reason", "operational_failure")
                .counter()
                .count());
    }

    @Test
    void deveResolverOrdemPeloPayloadQuandoPagamentoUsaAggregateIdProprio() {
        var ordem = store.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                "Resolver aggregate financeiro");
        var ordemServicoId = ordem.ordemServicoId();
        var execucaoId = UUID.randomUUID();
        var orcamentoId = UUID.randomUUID();
        var pagamentoId = UUID.randomUUID();

        store.consumirEvento(evento("diagnosticoFinalizado", ordemServicoId));
        store.consumirEvento(evento("orcamentoGerado", ordemServicoId));
        store.consumirEvento(evento("orcamentoAprovado", ordemServicoId));
        store.consumirEvento(evento("execucaoIniciada", ordemServicoId));
        store.consumirEvento(evento("execucaoFinalizada", ordemServicoId));
        store.consumirEvento(evento(
                "pagamentoSolicitado", pagamentoId, ordemServicoId,
                Map.of("execucaoId", execucaoId.toString(), "orcamentoId", orcamentoId.toString(),
                        "pagamentoId", pagamentoId.toString())));
        var saga = store.consumirEvento(evento(
                "pagamentoConfirmado", pagamentoId, ordemServicoId,
                Map.of("pagamentoId", pagamentoId.toString())));

        assertEquals(EstadoSaga.AGUARDANDO_ENTREGA, saga.estado());
    }

    private static DomainEventEnvelope evento(String eventType, UUID ordemServicoId) {
        return evento(eventType, ordemServicoId, ordemServicoId, Map.of());
    }

    private static DomainEventEnvelope evento(
            String eventType,
            UUID aggregateId,
            UUID ordemServicoId,
            Map<String, Object> payload) {
        var completePayload = new java.util.HashMap<>(payload);
        completePayload.put("ordemServicoId", ordemServicoId.toString());
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "oficina-execution-service",
                aggregateId,
                completePayload);
    }
}

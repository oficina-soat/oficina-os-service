package br.com.oficina.os.framework.messaging;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.OrdemServicoRecord;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SagaOrchestrationTest {

    @Inject
    AtendimentoSeedStore store;

    @Inject
    DomainEventConsumer consumer;

    @Inject
    OutboxPublisher outboxPublisher;

    @Test
    void deveCriarSagaEPublicarEventoDeAberturaDaOrdemServico() {
        var ordem = novaOrdemServico("Saga deve iniciar com outbox");

        var saga = store.buscarSaga(ordem.ordemServicoId());
        assertNotNull(saga);
        assertEquals(EstadoSaga.INICIADA, saga.estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.RECEBIDA, saga.estadoOrdemServico());

        var abertura = store.listarOutbox().stream()
                .filter(event -> event.aggregateId().equals(ordem.ordemServicoId()))
                .filter(event -> event.eventType().equals("ordemDeServicoCriada"))
                .findFirst()
                .orElseThrow();
        assertEquals("oficina.os.ordem-de-servico-criada", abertura.topic());
        assertEquals("PENDING", abertura.status());
        assertEquals(ordem.ordemServicoId().toString(), abertura.payload().get("ordemServicoId"));

        var publicados = outboxPublisher.publicarPendentes();
        assertTrue(publicados.stream().anyMatch(event -> event.eventId().equals(abertura.eventId())));
        assertEquals("PUBLISHED", store.listarOutbox().stream()
                .filter(event -> event.eventId().equals(abertura.eventId()))
                .findFirst()
                .orElseThrow()
                .status());
    }

    @Test
    void deveOrquestrarFluxoFelizAteEntregaComEventosDeOsESaga() {
        var ordem = novaOrdemServico("Fluxo feliz da saga");
        var ordemServicoId = ordem.ordemServicoId();
        var execucaoId = UUID.randomUUID();
        var orcamentoId = UUID.randomUUID();
        var pagamentoId = UUID.randomUUID();

        consumer.consumir(evento("diagnosticoIniciado", ordemServicoId, Map.of("execucaoId", execucaoId.toString())));
        consumer.consumir(evento("diagnosticoFinalizado", ordemServicoId, Map.of("execucaoId", execucaoId.toString())));
        consumer.consumir(evento("orcamentoGerado", ordemServicoId, Map.of("orcamentoId", orcamentoId.toString())));
        consumer.consumir(evento("orcamentoAprovado", ordemServicoId, Map.of("orcamentoId", orcamentoId.toString())));
        consumer.consumir(evento("execucaoIniciada", ordemServicoId, Map.of("execucaoId", execucaoId.toString())));
        consumer.consumir(evento("execucaoFinalizada", ordemServicoId, Map.of("execucaoId", execucaoId.toString())));
        consumer.consumir(evento("pagamentoSolicitado", ordemServicoId, Map.of(
                "orcamentoId", orcamentoId.toString(),
                "pagamentoId", pagamentoId.toString())));
        consumer.consumir(evento("pagamentoConfirmado", ordemServicoId, Map.of("pagamentoId", pagamentoId.toString())));
        store.alterarEstado(ordemServicoId, TipoDeEstadoDaOrdemDeServico.ENTREGUE, "Cliente retirou o veiculo");

        var saga = store.buscarSaga(ordemServicoId);
        assertEquals(EstadoSaga.FINALIZADA_COM_SUCESSO, saga.estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.ENTREGUE, store.buscarOrdemServico(ordemServicoId).estado());
        assertEquals(execucaoId, saga.execucaoId());
        assertEquals(orcamentoId, saga.orcamentoId());
        assertEquals(pagamentoId, saga.pagamentoId());

        assertOutboxContem(ordemServicoId, "ordemDeServicoFinalizada", "oficina.os.ordem-de-servico-finalizada");
        assertOutboxContem(ordemServicoId, "ordemDeServicoEntregue", "oficina.os.ordem-de-servico-entregue");
        assertOutboxContem(ordemServicoId, "sagaFinalizadaComSucesso", "oficina.saga.saga-finalizada-com-sucesso");
    }

    @Test
    void deveRetornarParaDiagnosticoQuandoOrcamentoForRecusadoSemCompensarSaga() {
        var ordem = novaOrdemServico("Orcamento recusado na saga");
        var ordemServicoId = ordem.ordemServicoId();
        var orcamentoId = UUID.randomUUID();

        consumer.consumir(evento("diagnosticoIniciado", ordemServicoId, Map.of()));
        consumer.consumir(evento("diagnosticoFinalizado", ordemServicoId, Map.of()));
        consumer.consumir(evento("orcamentoGerado", ordemServicoId, Map.of("orcamentoId", orcamentoId.toString())));
        consumer.consumir(evento("orcamentoRecusado", ordemServicoId, Map.of(
                "orcamentoId", orcamentoId.toString(),
                "motivo", "Cliente pediu revisao")));

        var saga = store.buscarSaga(ordemServicoId);
        assertEquals(EstadoSaga.EM_DIAGNOSTICO, saga.estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, store.buscarOrdemServico(ordemServicoId).estado());
        assertTrue(store.listarOutbox().stream()
                .noneMatch(event -> event.aggregateId().equals(ordemServicoId)
                        && event.eventType().equals("sagaCompensada")));
    }

    @Test
    void deveConsumirEventoDeFormaIdempotente() {
        var ordem = novaOrdemServico("Idempotencia no consumo da saga");
        var evento = evento("diagnosticoIniciado", ordem.ordemServicoId(), Map.of());

        consumer.consumir(evento);
        consumer.consumir(evento);

        assertEquals(EstadoSaga.EM_DIAGNOSTICO, store.buscarSaga(ordem.ordemServicoId()).estado());
        assertEquals(2, store.historicoSaga(ordem.ordemServicoId()).size());
        assertEquals(2, store.historico(ordem.ordemServicoId()).size());
    }

    @Test
    void devePublicarSagaCompensadaQuandoCancelamentoForSolicitado() {
        var ordem = novaOrdemServico("Cancelamento compensado pela saga");

        store.cancelar(ordem.ordemServicoId(), "Cliente cancelou atendimento");

        assertEquals(EstadoSaga.COMPENSADA, store.buscarSaga(ordem.ordemServicoId()).estado());
        assertOutboxContem(ordem.ordemServicoId(), "sagaCompensada", "oficina.saga.saga-compensada");
    }

    private OrdemServicoRecord novaOrdemServico(String descricao) {
        return store.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                descricao);
    }

    private DomainEventEnvelope evento(String eventType, UUID ordemServicoId, Map<String, Object> payload) {
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

    private void assertOutboxContem(UUID ordemServicoId, String eventType, String topic) {
        var event = store.listarOutbox().stream()
                .filter(candidate -> candidate.aggregateId().equals(ordemServicoId))
                .filter(candidate -> candidate.eventType().equals(eventType))
                .findFirst()
                .orElseThrow();
        assertEquals(topic, event.topic());
        assertEquals("PENDING", event.status());
    }
}

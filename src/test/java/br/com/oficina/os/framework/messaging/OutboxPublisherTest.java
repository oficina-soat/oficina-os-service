package br.com.oficina.os.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import br.com.oficina.os.core.usecases.outbox.PublicarEventosPendentesUseCase;
import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxPublisherTest {

    @Test
    void deveCalcularMultiplicadorDeRetryComLimiteInferior() {
        assertEquals(1L, OutboxPublisher.retryMultiplier(0));
        assertEquals(1L, OutboxPublisher.retryMultiplier(1));
    }

    @Test
    void deveCalcularMultiplicadorDeRetryComLimiteSuperior() {
        assertEquals(2L, OutboxPublisher.retryMultiplier(2));
        assertEquals(1024L, OutboxPublisher.retryMultiplier(20));
    }

    @Test
    void devePublicarEventoPendenteComAtributosContratados() {
        var gateway = new AtendimentoSeedStore();
        var event = criarEventoPendente(gateway);
        var messagingClient = new RecordingMessagingClient();
        var publisher = publisher(gateway, messagingClient, 3);

        var publicados = publisher.publicarPendentes();

        assertTrue(publicados.stream().anyMatch(candidate -> candidate.eventId().equals(event.eventId())));
        assertEquals("PUBLISHED", buscarOutbox(gateway, event.eventId()).status());
        assertEquals(1, messagingClient.messages.size());
        var message = messagingClient.messages.getFirst();
        assertEquals("oficina.os.ordem-de-servico-criada", message.topic());
        assertEquals("ordemDeServicoCriada", message.attributes().get("eventType"));
        assertEquals("oficina-os-service", message.attributes().get("producer"));
        assertEquals(event.aggregateId().toString(), message.attributes().get("aggregateId"));
        assertEquals(event.correlationId(), message.attributes().get("correlationId"));
        assertTrue(message.body().contains("ordemDeServicoCriada"));
    }

    @Test
    void deveRegistrarFalhaFinalQuandoPublicacaoFalhar() {
        var gateway = new AtendimentoSeedStore();
        var event = criarEventoPendente(gateway);
        var messagingClient = new RecordingMessagingClient();
        messagingClient.failure = new IllegalStateException(
                "sns indisponivel",
                new IllegalArgumentException("endpoint sem resposta"));
        var publisher = publisher(gateway, messagingClient, 1);

        var publicados = publisher.publicarPendentes();

        assertTrue(publicados.isEmpty());
        var failure = buscarOutbox(gateway, event.eventId());
        assertEquals("FAILED", failure.status());
        assertEquals(1, failure.attempts());
        assertEquals("endpoint sem resposta", failure.lastError());
        assertTrue(messagingClient.messages.isEmpty());
    }

    private static OutboxPublisher publisher(
            AtendimentoGateway gateway,
            RecordingMessagingClient messagingClient,
            int maxAttempts) {
        return new OutboxPublisher(
                gateway,
                new PublicarEventosPendentesUseCase(gateway),
                messagingClient,
                new DomainEventJsonCodec(new ObjectMapper().findAndRegisterModules()),
                true,
                10,
                maxAttempts,
                1);
    }

    private static OutboxEventRecord criarEventoPendente(AtendimentoGateway gateway) {
        var ordem = gateway.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                "Publicacao unit test");
        return gateway.listarOutbox().stream()
                .filter(candidate -> candidate.aggregateId().equals(ordem.ordemServicoId()))
                .findFirst()
                .orElseThrow();
    }

    private static OutboxEventRecord buscarOutbox(AtendimentoGateway gateway, UUID eventId) {
        return gateway.listarOutbox().stream()
                .filter(candidate -> candidate.eventId().equals(eventId))
                .findFirst()
                .orElseThrow();
    }

    private static final class RecordingMessagingClient extends AwsDomainMessagingClient {
        private final List<PublishedMessage> messages = new ArrayList<>();
        private RuntimeException failure;

        private RecordingMessagingClient() {
            super(
                    DomainMessagingRoutes.SERVICE_NAME,
                    "us-east-1",
                    "http://localhost:4566",
                    Optional.of("000000000000"),
                    Optional.of("test"),
                    Optional.of("test"));
        }

        @Override
        void publish(String topic, String message, Map<String, String> attributes) {
            if (failure != null) {
                throw failure;
            }
            messages.add(new PublishedMessage(topic, message, Map.copyOf(attributes)));
        }
    }

    private record PublishedMessage(String topic, String body, Map<String, String> attributes) {
    }
}

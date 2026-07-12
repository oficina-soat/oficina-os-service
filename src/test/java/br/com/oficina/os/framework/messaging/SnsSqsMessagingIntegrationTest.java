package br.com.oficina.os.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@QuarkusTest
@QuarkusTestResource(LocalStackMessagingTestResource.class)
class SnsSqsMessagingIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Inject
    AtendimentoSeedStore store;

    @Inject
    OutboxPublisher outboxPublisher;

    @Inject
    SqsDomainEventConsumer sqsConsumer;

    @Test
    void devePublicarOutboxNoSnsEEntregarNaFilaConsumidora() throws Exception {
        var ordem = store.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                "Publicacao SNS/SQS");
        var event = store.listarOutbox().stream()
                .filter(candidate -> candidate.aggregateId().equals(ordem.ordemServicoId()))
                .filter(candidate -> candidate.eventType().equals("ordemDeServicoCriada"))
                .findFirst()
                .orElseThrow();

        var publicados = outboxPublisher.publicarPendentes();

        assertTrue(publicados.stream().anyMatch(candidate -> candidate.eventId().equals(event.eventId())));
        assertEquals("PUBLISHED", store.listarOutbox().stream()
                .filter(candidate -> candidate.eventId().equals(event.eventId()))
                .findFirst()
                .orElseThrow()
                .status());

        try (var sqs = LocalStackMessagingTestResource.sqsClient()) {
            var queueUrl = queueUrl(sqs, "oficina.os.ordem-de-servico-criada", "oficina-billing-service");
            var messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(2)
                    .build()).messages();
            assertEquals(1, messages.size());
            assertEquals("ordemDeServicoCriada", JSON.readTree(messages.getFirst().body()).path("eventType").asText());
        }
    }

    @Test
    void deveConsumirSqsEAckSomenteAposProcessarEvento() {
        var ordem = store.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                "Consumo SQS");
        var evento = new DomainEventEnvelope(
                UUID.randomUUID(),
                "diagnosticoIniciado",
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "oficina-execution-service",
                ordem.ordemServicoId(),
                Map.of("ordemServicoId", ordem.ordemServicoId().toString(), "execucaoId", UUID.randomUUID().toString()));

        try (var sns = LocalStackMessagingTestResource.snsClient()) {
            var message = JSON.writeValueAsString(evento);
            sns.publish(builder -> builder
                    .topicArn(LocalStackMessagingTestResource.topicArn("oficina.execution.diagnostico-iniciado"))
                    .message(message));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }

        assertEquals(1, sqsConsumer.consumirDisponiveis());

        try (var sqs = LocalStackMessagingTestResource.sqsClient()) {
            var queueUrl = queueUrl(sqs, "oficina.execution.diagnostico-iniciado", "oficina-os-service");
            var messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(1)
                    .build()).messages();
            assertTrue(messages.isEmpty());
        }
    }

    private static String queueUrl(software.amazon.awssdk.services.sqs.SqsClient sqs, String topic, String consumer) {
        return sqs.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(LocalStackMessagingTestResource.queueName(topic, consumer))
                .build()).queueUrl();
    }
}

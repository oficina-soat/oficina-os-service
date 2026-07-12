package br.com.oficina.os.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainEventJsonCodecTest {
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 7, 12, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void deveCodificarRegistroDeOutboxComoEnvelopeDeDominio() {
        var codec = codec();
        var event = outbox();

        var decoded = codec.decode(codec.encode(event));

        assertEquals(event.eventId(), decoded.eventId());
        assertEquals(event.eventType(), decoded.eventType());
        assertEquals(event.eventVersion(), decoded.eventVersion());
        assertEquals(event.producer(), decoded.producer());
        assertEquals(event.aggregateId(), decoded.aggregateId());
        assertEquals("CRIADA", decoded.payload().get("status"));
    }

    @Test
    void deveDecodificarEnvelopeTextualDoSns() throws Exception {
        var codec = codec();
        var encoded = codec.encode(outbox());
        var snsEnvelope = new ObjectMapper().writeValueAsString(Map.of("Message", encoded));

        var decoded = codec.decode(snsEnvelope);

        assertEquals("ordemDeServicoCriada", decoded.eventType());
    }

    @Test
    void deveRejeitarMensagemInvalida() {
        var codec = codec();

        assertThrows(IllegalArgumentException.class, () -> codec.decode("{"));
    }

    private static DomainEventJsonCodec codec() {
        return new DomainEventJsonCodec(new ObjectMapper().findAndRegisterModules());
    }

    private static OutboxEventRecord outbox() {
        var aggregateId = UUID.randomUUID();
        return new OutboxEventRecord(
                UUID.randomUUID(),
                aggregateId,
                "ordemDeServicoCriada",
                1,
                "oficina.os.ordem-de-servico-criada",
                "oficina-os-service",
                Map.of("status", "CRIADA"),
                "PENDING",
                "corr-os-codec",
                NOW,
                NOW,
                null,
                0,
                null);
    }
}

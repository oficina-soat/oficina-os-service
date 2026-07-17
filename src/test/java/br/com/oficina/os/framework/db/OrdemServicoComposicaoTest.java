package br.com.oficina.os.framework.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ItemPecaRecord;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ItemServicoRecord;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrdemServicoComposicaoTest {
    @Test
    void devePersistirSnapshotsEEventosDuranteDiagnostico() {
        var gateway = new InMemoryAtendimentoGateway();
        iniciarDiagnostico(gateway);
        var servicoId = UUID.randomUUID();
        var pecaId = UUID.randomUUID();

        gateway.incluirServico(gateway.SEED_ORDEM_SERVICO_ID,
                new ItemServicoRecord(servicoId, "Diagnostico", new BigDecimal("1.5"), new BigDecimal("100.00")), "corr-1");
        var ordem = gateway.incluirPeca(gateway.SEED_ORDEM_SERVICO_ID,
                new ItemPecaRecord(pecaId, "Filtro", new BigDecimal("2"), new BigDecimal("30.00")), "corr-1");

        assertEquals(0, ordem.servicos().getFirst().valorTotal().compareTo(new BigDecimal("150.000")));
        assertEquals(0, ordem.pecas().getFirst().valorTotal().compareTo(new BigDecimal("60.00")));
        var eventTypes = gateway.listarOutbox().stream().map(event -> event.eventType()).toList();
        assertTrue(eventTypes.contains("servicoIncluidoNaOrdemDeServico"));
        assertTrue(eventTypes.contains("pecaIncluidaNaOrdemDeServico"));
    }

    @Test
    void deveRejeitarInclusaoForaDoDiagnosticoEDuplicada() {
        var gateway = new InMemoryAtendimentoGateway();
        var item = new ItemServicoRecord(UUID.randomUUID(), "Alinhamento", BigDecimal.ONE, BigDecimal.TEN);

        assertThrows(WebApplicationException.class,
                () -> gateway.incluirServico(gateway.SEED_ORDEM_SERVICO_ID, item, "corr"));

        iniciarDiagnostico(gateway);
        gateway.incluirServico(gateway.SEED_ORDEM_SERVICO_ID, item, "corr");
        assertThrows(WebApplicationException.class,
                () -> gateway.incluirServico(gateway.SEED_ORDEM_SERVICO_ID, item, "corr"));
    }

    private static void iniciarDiagnostico(InMemoryAtendimentoGateway gateway) {
        gateway.consumirEvento(new DomainEventEnvelope(
                UUID.randomUUID(),
                "diagnosticoIniciado",
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "oficina-execution-service",
                gateway.SEED_ORDEM_SERVICO_ID,
                Map.of("ordemServicoId", gateway.SEED_ORDEM_SERVICO_ID.toString(), "execucaoId", UUID.randomUUID().toString())));
    }
}

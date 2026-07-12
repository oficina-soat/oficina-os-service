package br.com.oficina.os.framework.db;

import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.PAYLOAD_EXECUCAO_ID;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.PAYLOAD_MOTIVO;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.PAYLOAD_ORCAMENTO_ID;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.PAYLOAD_ORDEM_SERVICO_ID;
import static br.com.oficina.os.framework.db.AtendimentoGatewaySupport.PAYLOAD_PAGAMENTO_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryAtendimentoGatewayTest {

    @Test
    void deveGerenciarCadastroOrdemOutboxEPublicacao() {
        var gateway = new InMemoryAtendimentoGateway();
        var clienteInexistenteId = UUID.randomUUID();

        assertFalse(gateway.listarClientes().isEmpty());
        assertThrows(NotFoundException.class, () -> gateway.buscarCliente(clienteInexistenteId));

        var cliente = gateway.criarCliente("  Ana Souza  ", "84191404067", "  ", "ana@example.com");
        assertEquals("Ana Souza", cliente.nome());
        assertNull(cliente.telefone());

        var clienteAtualizado = gateway.atualizarCliente(
                cliente.clienteId(),
                " Ana Maria ",
                "84191404067",
                "+5511999999999",
                "ana.maria@example.com");
        assertEquals("Ana Maria", clienteAtualizado.nome());
        assertEquals("+5511999999999", clienteAtualizado.telefone());

        var veiculo = gateway.criarVeiculo(cliente.clienteId(), "abc1d23", " Honda ", " Fit ", 2021);
        assertEquals("ABC1D23", veiculo.placa());
        assertEquals(1, gateway.listarVeiculosDoCliente(cliente.clienteId()).size());

        var veiculoAtualizado = gateway.atualizarVeiculo(veiculo.veiculoId(), "def2g34", "Toyota", "Corolla", 2024);
        assertEquals("DEF2G34", veiculoAtualizado.placa());

        var clienteId = cliente.clienteId();
        var veiculoId = veiculo.veiculoId();
        assertThrows(WebApplicationException.class, () -> gateway.criarOrdemServico(
                AtendimentoGateway.SEED_CLIENTE_ID,
                veiculoId,
                "Nao liga"));
        assertThrows(IllegalArgumentException.class, () -> gateway.criarOrdemServico(
                clienteId,
                veiculoId,
                "  "));

        var ordem = gateway.criarOrdemServico(clienteId, veiculoId, " Barulho no motor ");
        assertEquals(TipoDeEstadoDaOrdemDeServico.RECEBIDA, ordem.estado());
        assertEquals(1, gateway.historico(ordem.ordemServicoId()).size());
        assertTrue(gateway.listarOrdensServico(TipoDeEstadoDaOrdemDeServico.RECEBIDA).contains(ordem));

        var pendentes = gateway.listarEventosPendentesParaPublicacao(0);
        assertFalse(pendentes.isEmpty());

        var publicado = gateway.marcarEventoPublicado(pendentes.get(0).eventId());
        assertEquals("PUBLISHED", publicado.status());
        assertEquals(1, publicado.attempts());

        var tentativa = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1);
        var pendenteNovamente = gateway.marcarFalhaPublicacao(publicado.eventId(), "erro transitorio", tentativa, false);
        assertEquals("PENDING", pendenteNovamente.status());
        assertEquals("erro transitorio", pendenteNovamente.lastError());

        var falhaFinal = gateway.marcarFalhaPublicacao(publicado.eventId(), "erro definitivo", tentativa, true);
        assertEquals("FAILED", falhaFinal.status());

        var eventoInexistenteId = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> gateway.marcarEventoPublicado(eventoInexistenteId));
    }

    @Test
    void deveConsumirEventosDaSagaEIgnorarDuplicados() {
        var gateway = new InMemoryAtendimentoGateway();
        var ordem = gateway.criarOrdemServico(
                AtendimentoGateway.SEED_CLIENTE_ID,
                AtendimentoGateway.SEED_VEICULO_ID,
                "Falha intermitente");
        var ordemServicoId = ordem.ordemServicoId();
        var execucaoId = UUID.randomUUID();
        var orcamentoId = UUID.randomUUID();
        var pagamentoId = UUID.randomUUID();

        var diagnosticoIniciado = evento("diagnosticoIniciado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId.toString(),
                PAYLOAD_EXECUCAO_ID, execucaoId.toString()));
        var saga = gateway.consumirEvento(diagnosticoIniciado);
        assertEquals(EstadoSaga.EM_DIAGNOSTICO, saga.estado());
        assertEquals(saga.sagaId(), gateway.consumirEvento(diagnosticoIniciado).sagaId());

        gateway.consumirEvento(evento("diagnosticoFinalizado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_EXECUCAO_ID, execucaoId)));
        gateway.consumirEvento(evento("orcamentoGerado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_ORCAMENTO_ID, orcamentoId)));
        gateway.consumirEvento(evento("orcamentoAprovado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_ORCAMENTO_ID, orcamentoId)));
        gateway.consumirEvento(evento("execucaoIniciada", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_EXECUCAO_ID, execucaoId)));
        gateway.consumirEvento(evento("execucaoFinalizada", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_EXECUCAO_ID, execucaoId)));
        gateway.consumirEvento(evento("pagamentoSolicitado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_ORCAMENTO_ID, orcamentoId,
                PAYLOAD_PAGAMENTO_ID, pagamentoId)));
        gateway.consumirEvento(evento("pagamentoRecusado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_PAGAMENTO_ID, pagamentoId,
                PAYLOAD_MOTIVO, "Pagamento recusado")));
        saga = gateway.consumirEvento(evento("pagamentoConfirmado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_PAGAMENTO_ID, pagamentoId)));

        assertEquals(EstadoSaga.AGUARDANDO_ENTREGA, saga.estado());
        assertEquals(EstadoSaga.AGUARDANDO_ENTREGA, gateway.consumirEvento(
                evento("eventoDesconhecido", ordemServicoId, Map.of(PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId)))
                .estado());

        var entregue = gateway.alterarEstado(ordemServicoId, TipoDeEstadoDaOrdemDeServico.ENTREGUE, "Retirado");
        assertEquals(TipoDeEstadoDaOrdemDeServico.ENTREGUE, entregue.estado());
        assertEquals(EstadoSaga.FINALIZADA_COM_SUCESSO, gateway.buscarSaga(ordemServicoId).estado());
        assertFalse(gateway.historicoSaga(ordemServicoId).isEmpty());
    }

    @Test
    void deveProcessarOrcamentoRecusadoECompensarCancelamentoUmaVez() {
        var gateway = new InMemoryAtendimentoGateway();
        var ordem = gateway.criarOrdemServico(
                AtendimentoGateway.SEED_CLIENTE_ID,
                AtendimentoGateway.SEED_VEICULO_ID,
                "Ruido na suspensao");
        var ordemServicoId = ordem.ordemServicoId();
        var execucaoId = UUID.randomUUID();
        var orcamentoId = UUID.randomUUID();

        gateway.consumirEvento(evento("diagnosticoIniciado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_EXECUCAO_ID, execucaoId)));
        gateway.consumirEvento(evento("diagnosticoFinalizado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_EXECUCAO_ID, execucaoId)));
        gateway.consumirEvento(evento("orcamentoGerado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_ORCAMENTO_ID, orcamentoId)));

        var recusada = gateway.consumirEvento(evento("orcamentoRecusado", ordemServicoId, Map.of(
                PAYLOAD_ORDEM_SERVICO_ID, ordemServicoId,
                PAYLOAD_ORCAMENTO_ID, orcamentoId,
                PAYLOAD_MOTIVO, "Valor nao aprovado")));

        assertEquals(EstadoSaga.EM_DIAGNOSTICO, recusada.estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO, gateway.buscarOrdemServico(ordemServicoId).estado());

        var cancelamento = gateway.cancelar(ordemServicoId, "Cliente desistiu");
        assertEquals("ACEITO", cancelamento.status());
        assertEquals(EstadoSaga.COMPENSADA, gateway.buscarSaga(ordemServicoId).estado());

        var totalOutbox = gateway.listarOutbox().size();
        gateway.cancelar(ordemServicoId, "Nova tentativa");
        assertEquals(totalOutbox, gateway.listarOutbox().size());
    }

    private static DomainEventEnvelope evento(String eventType, UUID ordemServicoId, Map<String, Object> payload) {
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "oficina-test",
                ordemServicoId,
                payload);
    }
}

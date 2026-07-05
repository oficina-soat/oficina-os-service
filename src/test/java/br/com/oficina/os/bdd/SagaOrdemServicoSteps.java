package br.com.oficina.os.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import br.com.oficina.os.framework.messaging.DomainEventEnvelope;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SagaOrdemServicoSteps {
    private final AtendimentoSeedStore store = new AtendimentoSeedStore();
    private AtendimentoSeedStore.OrdemServicoRecord ordem;
    private UUID execucaoId;
    private UUID orcamentoId;
    private UUID pagamentoId;

    @Dado("existe uma ordem de serviço recebida")
    public void existeUmaOrdemDeServicoRecebida() {
        ordem = store.criarOrdemServico(
                AtendimentoSeedStore.SEED_CLIENTE_ID,
                AtendimentoSeedStore.SEED_VEICULO_ID,
                "Veiculo nao liga durante cenário BDD");

        assertEquals(TipoDeEstadoDaOrdemDeServico.RECEBIDA, ordem.estado());
        assertOutboxContem("ordemDeServicoCriada", "oficina.os.ordem-de-servico-criada");
    }

    @Quando("o diagnóstico é concluído pelo serviço de execução")
    public void oDiagnosticoEConcluidoPeloServicoDeExecucao() {
        execucaoId = UUID.randomUUID();

        consumir("diagnosticoIniciado", "oficina-execution-service", Map.of("execucaoId", execucaoId.toString()));
        consumir("diagnosticoFinalizado", "oficina-execution-service", Map.of(
                "execucaoId", execucaoId.toString(),
                "diagnostico", "Bateria sem carga",
                "servicos", java.util.List.of(),
                "pecas", java.util.List.of()));

        assertEquals(EstadoSaga.AGUARDANDO_ORCAMENTO, store.buscarSaga(ordem.ordemServicoId()).estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO, store.buscarOrdemServico(ordem.ordemServicoId()).estado());
    }

    @Quando("o orçamento é gerado e aprovado pelo serviço financeiro")
    public void oOrcamentoEGeradoEAprovadoPeloServicoFinanceiro() {
        orcamentoId = UUID.randomUUID();

        consumir("orcamentoGerado", "oficina-billing-service", Map.of("orcamentoId", orcamentoId.toString()));
        consumir("orcamentoAprovado", "oficina-billing-service", Map.of("orcamentoId", orcamentoId.toString()));

        var saga = store.buscarSaga(ordem.ordemServicoId());
        assertEquals(EstadoSaga.EM_EXECUCAO, saga.estado());
        assertEquals(orcamentoId, saga.orcamentoId());
    }

    @Quando("a execução técnica é finalizada")
    public void aExecucaoTecnicaEFinalizada() {
        aExecucaoTecnicaEIniciadaPeloServicoDeExecucao();
        consumir("execucaoFinalizada", "oficina-execution-service", Map.of("execucaoId", execucaoId.toString()));

        assertEquals(EstadoSaga.AGUARDANDO_PAGAMENTO, store.buscarSaga(ordem.ordemServicoId()).estado());
        assertOutboxContem("ordemDeServicoFinalizada", "oficina.os.ordem-de-servico-finalizada");
    }

    @Quando("a execução técnica é iniciada pelo serviço de execução")
    public void aExecucaoTecnicaEIniciadaPeloServicoDeExecucao() {
        consumir("execucaoIniciada", "oficina-execution-service", Map.of("execucaoId", execucaoId.toString()));

        assertEquals(EstadoSaga.EM_EXECUCAO, store.buscarSaga(ordem.ordemServicoId()).estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO, store.buscarOrdemServico(ordem.ordemServicoId()).estado());
    }

    @Quando("uma falha operacional impede a continuidade antes da finalização")
    public void umaFalhaOperacionalImpedeAContinuidadeAntesDaFinalizacao() {
        store.cancelar(ordem.ordemServicoId(), "Falha definitiva de estoque antes da execucao finalizar");
    }

    @Quando("o pagamento é solicitado e confirmado")
    public void oPagamentoESolicitadoEConfirmado() {
        pagamentoId = UUID.randomUUID();

        consumir("pagamentoSolicitado", "oficina-billing-service", Map.of(
                "orcamentoId", orcamentoId.toString(),
                "pagamentoId", pagamentoId.toString()));
        consumir("pagamentoConfirmado", "oficina-billing-service", Map.of("pagamentoId", pagamentoId.toString()));

        var saga = store.buscarSaga(ordem.ordemServicoId());
        assertEquals(EstadoSaga.AGUARDANDO_ENTREGA, saga.estado());
        assertEquals(pagamentoId, saga.pagamentoId());
    }

    @Quando("o veículo é entregue ao cliente")
    public void oVeiculoEEntregueAoCliente() {
        store.alterarEstado(ordem.ordemServicoId(), TipoDeEstadoDaOrdemDeServico.ENTREGUE, "Cliente retirou o veiculo");
    }

    @Entao("a saga da ordem de serviço deve finalizar com sucesso")
    public void aSagaDaOrdemDeServicoDeveFinalizarComSucesso() {
        var saga = store.buscarSaga(ordem.ordemServicoId());

        assertNotNull(saga);
        assertEquals(EstadoSaga.FINALIZADA_COM_SUCESSO, saga.estado());
        assertEquals(TipoDeEstadoDaOrdemDeServico.ENTREGUE, store.buscarOrdemServico(ordem.ordemServicoId()).estado());
        assertEquals(execucaoId, saga.execucaoId());
        assertEquals(orcamentoId, saga.orcamentoId());
        assertEquals(pagamentoId, saga.pagamentoId());
    }

    @Entao("os eventos finais de OS e Saga devem ser publicados")
    public void osEventosFinaisDeOsESagaDevemSerPublicados() {
        assertOutboxContem("ordemDeServicoEntregue", "oficina.os.ordem-de-servico-entregue");
        assertOutboxContem("sagaFinalizadaComSucesso", "oficina.saga.saga-finalizada-com-sucesso");
    }

    @Entao("a saga da ordem de serviço deve ser compensada")
    public void aSagaDaOrdemDeServicoDeveSerCompensada() {
        var saga = store.buscarSaga(ordem.ordemServicoId());

        assertNotNull(saga);
        assertEquals(EstadoSaga.COMPENSADA, saga.estado());
        assertEquals(execucaoId, saga.execucaoId());
    }

    @Entao("o evento de compensação da Saga deve ser publicado")
    public void oEventoDeCompensacaoDaSagaDeveSerPublicado() {
        assertOutboxContem("sagaCompensada", "oficina.saga.saga-compensada");
    }

    private void consumir(String eventType, String producer, Map<String, Object> payload) {
        var completo = new LinkedHashMap<String, Object>();
        completo.put("ordemServicoId", ordem.ordemServicoId().toString());
        completo.putAll(payload);

        store.consumirEvento(new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                producer,
                ordem.ordemServicoId(),
                completo));
    }

    private void assertOutboxContem(String eventType, String topic) {
        assertTrue(store.listarOutbox().stream()
                .filter(event -> event.aggregateId().equals(ordem.ordemServicoId()))
                .anyMatch(event -> event.eventType().equals(eventType) && event.topic().equals(topic)));
    }
}

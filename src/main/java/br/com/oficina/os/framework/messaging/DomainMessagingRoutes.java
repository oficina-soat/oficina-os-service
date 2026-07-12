package br.com.oficina.os.framework.messaging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DomainMessagingRoutes {
    static final String SERVICE_NAME = "oficina-os-service";

    private static final Map<String, String> PRODUCED_TOPICS = orderedMap(Map.ofEntries(
            Map.entry("ordemDeServicoCriada", "oficina.os.ordem-de-servico-criada"),
            Map.entry("pecaIncluidaNaOrdemDeServico", "oficina.os.peca-incluida-na-ordem-de-servico"),
            Map.entry("servicoIncluidoNaOrdemDeServico", "oficina.os.servico-incluido-na-ordem-de-servico"),
            Map.entry("ordemDeServicoFinalizada", "oficina.os.ordem-de-servico-finalizada"),
            Map.entry("ordemDeServicoEntregue", "oficina.os.ordem-de-servico-entregue"),
            Map.entry("sagaCompensada", "oficina.saga.saga-compensada"),
            Map.entry("sagaFinalizadaComSucesso", "oficina.saga.saga-finalizada-com-sucesso"),
            Map.entry("usuarioAdicionado", "oficina.os.usuario-adicionado"),
            Map.entry("usuarioAtualizado", "oficina.os.usuario-atualizado"),
            Map.entry("usuarioExcluido", "oficina.os.usuario-excluido")));

    private static final Map<String, String> CONSUMED_TOPICS = orderedMap(Map.ofEntries(
            Map.entry("diagnosticoIniciado", "oficina.execution.diagnostico-iniciado"),
            Map.entry("diagnosticoFinalizado", "oficina.execution.diagnostico-finalizado"),
            Map.entry("orcamentoGerado", "oficina.billing.orcamento-gerado"),
            Map.entry("orcamentoAprovado", "oficina.billing.orcamento-aprovado"),
            Map.entry("orcamentoRecusado", "oficina.billing.orcamento-recusado"),
            Map.entry("execucaoIniciada", "oficina.execution.execucao-iniciada"),
            Map.entry("execucaoFinalizada", "oficina.execution.execucao-finalizada"),
            Map.entry("pagamentoSolicitado", "oficina.billing.pagamento-solicitado"),
            Map.entry("pagamentoConfirmado", "oficina.billing.pagamento-confirmado"),
            Map.entry("pagamentoRecusado", "oficina.billing.pagamento-recusado")));

    private DomainMessagingRoutes() {
    }

    static boolean isProduced(String eventType, String topic) {
        return topic.equals(PRODUCED_TOPICS.get(eventType));
    }

    static List<String> consumedTopics() {
        return List.copyOf(CONSUMED_TOPICS.values());
    }

    static List<String> producedTopics() {
        return List.copyOf(PRODUCED_TOPICS.values());
    }

    static String queueName(String topic) {
        return physicalName(topic + "." + SERVICE_NAME);
    }

    static String physicalName(String logicalName) {
        return logicalName.replace('.', '-');
    }

    private static Map<String, String> orderedMap(Map<String, String> values) {
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(
                        LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }
}

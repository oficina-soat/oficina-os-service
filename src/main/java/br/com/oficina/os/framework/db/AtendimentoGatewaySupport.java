package br.com.oficina.os.framework.db;

import br.com.oficina.os.core.entities.cliente.DocumentoFactory;
import br.com.oficina.os.core.entities.cliente.Email;
import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.entities.ordem_de_servico.TipoDeEstadoDaOrdemDeServico;
import br.com.oficina.os.core.entities.veiculo.MarcaDeVeiculo;
import br.com.oficina.os.core.entities.veiculo.ModeloDeVeiculo;
import br.com.oficina.os.core.entities.veiculo.PlacaDeVeiculo;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

final class AtendimentoGatewaySupport {
    static final String PRODUCER = "oficina-os-service";
    static final String EVENT_ORDEM_DE_SERVICO_CRIADA = "ordemDeServicoCriada";
    static final String EVENT_SAGA_COMPENSADA = "sagaCompensada";
    static final String PAYLOAD_ORDEM_SERVICO_ID = "ordemServicoId";
    static final String PAYLOAD_ESTADO_ATUAL = "estadoAtual";
    static final String PAYLOAD_EXECUCAO_ID = "execucaoId";
    static final String PAYLOAD_ORCAMENTO_ID = "orcamentoId";
    static final String PAYLOAD_PAGAMENTO_ID = "pagamentoId";
    static final String PAYLOAD_MOTIVO = "motivo";
    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_PUBLISHED = "PUBLISHED";
    static final String STATUS_FAILED = "FAILED";

    private AtendimentoGatewaySupport() {
    }

    static UUID uuidFromPayload(Map<String, Object> payload, String fieldName, UUID fallback) {
        var value = payload.get(fieldName);
        if (value == null) {
            return fallback;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    static String stringFromPayload(Map<String, Object> payload, String fieldName) {
        var value = payload.get(fieldName);
        return value == null ? null : value.toString();
    }

    static void validarCliente(String nome, String documento, String email) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do cliente e obrigatorio.");
        }
        DocumentoFactory.from(documento);
        if (email != null && !email.isBlank()) {
            new Email(email);
        }
    }

    static void validarVeiculo(String placa, String marca, String modelo, int ano) {
        new PlacaDeVeiculo(placa);
        new MarcaDeVeiculo(marca);
        new ModeloDeVeiculo(modelo);
        if (ano < 1900) {
            throw new IllegalArgumentException("Ano do veiculo deve ser maior ou igual a 1900.");
        }
    }

    static void validarTransicao(TipoDeEstadoDaOrdemDeServico atual, TipoDeEstadoDaOrdemDeServico novo) {
        boolean valida = switch (atual) {
            case RECEBIDA -> novo == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO -> novo == TipoDeEstadoDaOrdemDeServico.AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> novo == TipoDeEstadoDaOrdemDeServico.EM_EXECUCAO
                    || novo == TipoDeEstadoDaOrdemDeServico.EM_DIAGNOSTICO;
            case EM_EXECUCAO -> novo == TipoDeEstadoDaOrdemDeServico.FINALIZADA;
            case FINALIZADA -> novo == TipoDeEstadoDaOrdemDeServico.ENTREGUE;
            case ENTREGUE -> false;
        };
        if (!valida) {
            throw new WebApplicationException("Transicao de estado invalida: " + atual + " -> " + novo, Response.Status.CONFLICT);
        }
    }

    static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

record SagaExternalIds(
        UUID execucaoId,
        UUID orcamentoId,
        UUID pagamentoId) {
}

record SagaTransition(
        EstadoSaga novoEstado,
        TipoDeEstadoDaOrdemDeServico estadoOrdemServico,
        String etapa,
        String motivo,
        SagaExternalIds ids,
        OffsetDateTime ocorridoEm,
        String correlationId) {
}

package br.com.oficina.os.framework.observability;

import br.com.oficina.os.core.entities.ordem_de_servico.EstadoSaga;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.SagaRecord;
import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SagaObservability {
    private static final Logger LOG = Logger.getLogger(SagaObservability.class);
    private static final String SAGA_TYPE = "ordemServico";
    private final OperationalMetrics metrics;

    public SagaObservability(OperationalMetrics metrics) {
        this.metrics = metrics;
    }

    public void observe(SagaRecord previous, SagaRecord current) {
        if (current == null || sameTransition(previous, current)) {
            return;
        }
        var reason = categorizeReason(current);
        if (previous == null) {
            metrics.sagaStarted(SAGA_TYPE, current.ultimaEtapa());
        } else {
            metrics.sagaTransition(
                    SAGA_TYPE,
                    current.ultimaEtapa(),
                    terminalOutcome(current.estado()),
                    reason,
                    Duration.between(previous.atualizadoEm(), current.atualizadoEm()));
        }
        logTransition(previous, current, reason);
    }

    private static boolean sameTransition(SagaRecord previous, SagaRecord current) {
        return previous != null
                && previous.estado() == current.estado()
                && previous.ultimaEtapa().equals(current.ultimaEtapa());
    }

    private static String terminalOutcome(EstadoSaga state) {
        return switch (state) {
            case FINALIZADA_COM_SUCESSO -> "completed";
            case COMPENSADA -> "compensated";
            case FALHA_MANUAL -> "failed";
            default -> "in_progress";
        };
    }

    private static String categorizeReason(SagaRecord saga) {
        var value = (saga.ultimoErro() == null ? "" : saga.ultimoErro()).toLowerCase(Locale.ROOT);
        if (value.contains("orcamento") || value.contains("recus")) {
            return "business_rejection";
        }
        if (value.contains("estoque") || value.contains("execucao") || value.contains("operacional")) {
            return "operational_failure";
        }
        if (saga.estado() == EstadoSaga.COMPENSADA) {
            return "cancellation";
        }
        if (saga.estado() == EstadoSaga.FALHA_MANUAL) {
            return "manual_failure";
        }
        return "none";
    }

    private static void logTransition(SagaRecord previous, SagaRecord current, String reason) {
        var correlationId = current.correlationId() == null || current.correlationId().isBlank()
                ? current.sagaId().toString()
                : current.correlationId();
        var span = Span.current();
        span.setAttribute("sagaId", current.sagaId().toString());
        span.setAttribute("sagaStep", current.ultimaEtapa());
        span.setAttribute("aggregateId", current.ordemServicoId().toString());
        span.setAttribute("correlationId", correlationId);
        StructuredLog.info(LOG, "saga transition completed", Map.of(
                "correlationId", correlationId,
                "sagaId", current.sagaId().toString(),
                "ordemServicoId", current.ordemServicoId().toString(),
                "sagaState", current.estado().name(),
                "previousSagaState", previous == null ? "NONE" : previous.estado().name(),
                "sagaStep", current.ultimaEtapa(),
                "reason", reason));
    }
}

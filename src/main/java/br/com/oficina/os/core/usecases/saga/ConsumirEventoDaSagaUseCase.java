package br.com.oficina.os.core.usecases.saga;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.SagaRecord;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import java.util.concurrent.CompletableFuture;

public class ConsumirEventoDaSagaUseCase {
    private final AtendimentoGateway gateway;

    public ConsumirEventoDaSagaUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<SagaRecord> executar(DomainEventEnvelope event) {
        return CompletableFuture.completedFuture(gateway.consumirEvento(event));
    }
}

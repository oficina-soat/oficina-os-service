package br.com.oficina.os.core.usecases.saga;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.SagaRecord;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;

public class ConsumirEventoDaSagaUseCase {
    private final AtendimentoGateway gateway;

    public ConsumirEventoDaSagaUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public SagaRecord executar(DomainEventEnvelope event) {
        return gateway.consumirEvento(event);
    }
}

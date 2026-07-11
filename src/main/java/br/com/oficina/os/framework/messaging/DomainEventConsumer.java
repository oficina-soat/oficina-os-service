package br.com.oficina.os.framework.messaging;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.SagaRecord;
import br.com.oficina.os.core.interfaces.messaging.DomainEventEnvelope;
import br.com.oficina.os.core.usecases.saga.ConsumirEventoDaSagaUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DomainEventConsumer {
    private final ConsumirEventoDaSagaUseCase consumirEventoDaSaga;

    @Inject
    public DomainEventConsumer(ConsumirEventoDaSagaUseCase consumirEventoDaSaga) {
        this.consumirEventoDaSaga = consumirEventoDaSaga;
    }

    public SagaRecord consumir(DomainEventEnvelope event) {
        return consumirEventoDaSaga.executar(event).join();
    }
}

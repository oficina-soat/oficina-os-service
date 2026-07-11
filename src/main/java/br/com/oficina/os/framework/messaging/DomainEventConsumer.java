package br.com.oficina.os.framework.messaging;

import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DomainEventConsumer {
    private final AtendimentoSeedStore store;

    @Inject
    public DomainEventConsumer(AtendimentoSeedStore store) {
        this.store = store;
    }

    public AtendimentoSeedStore.SagaRecord consumir(DomainEventEnvelope event) {
        return store.consumirEvento(event);
    }
}

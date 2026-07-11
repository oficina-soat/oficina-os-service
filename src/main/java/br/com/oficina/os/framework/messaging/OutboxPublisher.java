package br.com.oficina.os.framework.messaging;

import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class OutboxPublisher {
    private final AtendimentoSeedStore store;

    @Inject
    public OutboxPublisher(AtendimentoSeedStore store) {
        this.store = store;
    }

    public List<OutboxEventRecord> publicarPendentes() {
        return store.publicarEventosPendentes();
    }
}

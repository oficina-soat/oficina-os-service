package br.com.oficina.os.framework.messaging;

import br.com.oficina.os.framework.db.AtendimentoSeedStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class OutboxPublisher {
    @Inject
    AtendimentoSeedStore store;

    public List<OutboxEventRecord> publicarPendentes() {
        return store.publicarEventosPendentes();
    }
}

package br.com.oficina.os.framework.messaging;

import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import br.com.oficina.os.core.usecases.outbox.PublicarEventosPendentesUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class OutboxPublisher {
    private final PublicarEventosPendentesUseCase publicarEventosPendentes;

    @Inject
    public OutboxPublisher(PublicarEventosPendentesUseCase publicarEventosPendentes) {
        this.publicarEventosPendentes = publicarEventosPendentes;
    }

    public List<OutboxEventRecord> publicarPendentes() {
        return publicarEventosPendentes.executar();
    }
}

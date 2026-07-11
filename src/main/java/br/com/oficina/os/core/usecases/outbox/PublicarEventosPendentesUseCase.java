package br.com.oficina.os.core.usecases.outbox;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PublicarEventosPendentesUseCase {
    private final AtendimentoGateway gateway;

    public PublicarEventosPendentesUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public CompletableFuture<List<OutboxEventRecord>> executar() {
        return CompletableFuture.completedFuture(gateway.publicarEventosPendentes());
    }
}

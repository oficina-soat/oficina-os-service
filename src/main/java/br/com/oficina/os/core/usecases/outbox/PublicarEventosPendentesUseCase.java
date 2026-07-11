package br.com.oficina.os.core.usecases.outbox;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.messaging.OutboxEventRecord;
import java.util.List;

public class PublicarEventosPendentesUseCase {
    private final AtendimentoGateway gateway;

    public PublicarEventosPendentesUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public List<OutboxEventRecord> executar() {
        return gateway.publicarEventosPendentes();
    }
}

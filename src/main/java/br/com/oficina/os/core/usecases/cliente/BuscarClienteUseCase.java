package br.com.oficina.os.core.usecases.cliente;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import java.util.UUID;

public class BuscarClienteUseCase {
    private final AtendimentoGateway gateway;

    public BuscarClienteUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public ClienteRecord executar(UUID clienteId) {
        return gateway.buscarCliente(clienteId);
    }
}

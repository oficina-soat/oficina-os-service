package br.com.oficina.os.core.usecases.cliente;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;
import java.util.UUID;

public class AtualizarClienteUseCase {
    private final AtendimentoGateway gateway;

    public AtualizarClienteUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public ClienteRecord executar(UUID clienteId, String nome, String documento, String telefone, String email) {
        return gateway.atualizarCliente(clienteId, nome, documento, telefone, email);
    }
}

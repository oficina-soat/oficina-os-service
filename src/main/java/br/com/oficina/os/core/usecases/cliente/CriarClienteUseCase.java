package br.com.oficina.os.core.usecases.cliente;

import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway;
import br.com.oficina.os.core.interfaces.gateway.AtendimentoGateway.ClienteRecord;

public class CriarClienteUseCase {
    private final AtendimentoGateway gateway;

    public CriarClienteUseCase(AtendimentoGateway gateway) {
        this.gateway = gateway;
    }

    public ClienteRecord executar(String nome, String documento, String telefone, String email) {
        return gateway.criarCliente(nome, documento, telefone, email);
    }
}
